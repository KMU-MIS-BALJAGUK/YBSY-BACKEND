package com.baljaguk.ybsy.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanApiTest {
    private static final String PLAN = """
            {"title":"운영체제","type":"class","credits":3,"startWeek":1,"endWeek":16,
             "hoursPerWeek":2.5,"studyDays":[0,4]}
            """;
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired ObjectMapper json;
    long owner;
    long semester;

    @BeforeEach
    void seedOwner() {
        owner = createUser();
        semester = createSemester(owner);
        jdbc.sql("insert into profiles(user_id,current_semester_id) values(?,?)").params(owner, semester).update();
    }

    private long createUser() {
        var sub = UUID.randomUUID().toString();
        jdbc.sql("insert into users(google_sub) values(?)").param(sub).update();
        return jdbc.sql("select id from users where google_sub=?").param(sub).query(Long.class).single();
    }

    private long createSemester(long userId) {
        jdbc.sql("insert into semesters(user_id) values(?)").param(userId).update();
        return jdbc.sql("select max(id) from semesters where user_id=?").param(userId).query(Long.class).single();
    }

    private ResultActions create(String path, String body) throws Exception {
        return mvc.perform(post(path).with(user(Long.toString(owner))).contentType("application/json").content(body));
    }

    private ResultActions change(long id, String body) throws Exception {
        return mvc.perform(patch("/plans/{id}", id).with(user(Long.toString(owner)))
                .contentType("application/json").content(body));
    }

    private long addOne() throws Exception {
        var result = create("/plans", PLAN).andExpect(status().isCreated())
                .andExpect(jsonPath("$.semesterId").value(semester)).andReturn();
        return json.readTree(result.getResponse().getContentAsByteArray()).get("id").asLong();
    }

    private String bulk(int count) {
        return "{\"plans\":[" + String.join(",", Collections.nCopies(count, PLAN)) + "]}";
    }

    private long count() {
        return jdbc.sql("select count(*) from plans where semester_id=?").param(semester).query(Long.class).single();
    }

    @Test
    @DisplayName("다른 사용자와 이전 학기의 계획은 목록에서 제외하고 조회·수정 시 404를 반환한다")
    void scope() throws Exception {
        long id = addOne();
        long other = createUser();
        long otherSemester = createSemester(other);
        jdbc.sql("insert into profiles(user_id,current_semester_id) values(?,?)").params(other, otherSemester).update();
        jdbc.sql("update profiles set current_semester_id=? where user_id=?").params(createSemester(owner), owner).update();
        for (long viewer : new long[]{owner, other}) {
            var principal = user(Long.toString(viewer));
            mvc.perform(get("/plans").with(principal)).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(get("/plans/{id}", id).with(principal)).andExpect(status().isNotFound());
            mvc.perform(patch("/plans/{id}", id).with(principal).contentType("application/json").content("{\"stopped\":true}"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Test
    @DisplayName("목록과 단건 조회는 본인의 현재 학기 계획을 반환한다")
    void readOwnPlans() throws Exception {
        long id = addOne();
        var principal = user(Long.toString(owner));
        mvc.perform(get("/plans").with(principal)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/plans/{id}", id).with(principal)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("운영체제")).andExpect(jsonPath("$.semesterId").value(semester));
    }

    @Test
    @DisplayName("다른 사용자의 유형은 계획에 지정할 수 없다")
    void rejectForeignCategory() throws Exception {
        long other = createUser();
        jdbc.sql("insert into plan_categories(user_id,name) values(?,?)").params(other, "공부").update();
        long category = jdbc.sql("select id from plan_categories where user_id=?").param(other).query(Long.class).single();
        change(addOne(), "{\"categoryId\":" + category + "}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("29개에서 일괄 두 건은 모두 거부하고 마지막 한 건은 등록한다")
    void bulkBoundary() throws Exception {
        create("/plans/bulk", bulk(29)).andExpect(status().isCreated())
                .andExpect(jsonPath("$[28].semesterId").value(semester));
        create("/plans/bulk", bulk(2)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_LIMIT_EXCEEDED"));
        addOne();
        assertThat(count()).isEqualTo(30);
    }

    @Test
    @DisplayName("일괄 등록 중 유효하지 않은 계획을 만나면 앞서 등록한 계획도 롤백한다")
    void rollback() throws Exception {
        create("/plans/bulk", "{\"plans\":[" + PLAN + "," + PLAN.replace("2.5", "2.3") + "]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        assertThat(count()).isZero();
    }

    @Test
    @DisplayName("마지막 한 자리에 단건과 일괄 요청이 동시에 들어오면 하나만 성공한다")
    void concurrentRequests() throws Exception {
        create("/plans/bulk", bulk(29)).andExpect(status().isCreated());
        var gate = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var single = pool.submit(() -> {
                gate.await(5, TimeUnit.SECONDS);
                return create("/plans", PLAN).andReturn().getResponse().getStatus();
            });
            var batch = pool.submit(() -> {
                gate.await(5, TimeUnit.SECONDS);
                return create("/plans/bulk", bulk(1)).andReturn().getResponse().getStatus();
            });
            assertThat(new Integer[]{single.get(10, TimeUnit.SECONDS), batch.get(10, TimeUnit.SECONDS)})
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(count()).isEqualTo(30);
    }

    @Test
    @DisplayName("중단한 뒤 일반 필드를 수정해도 중단 시각이 유지되고 재개하면 비워진다")
    void stopAndResume() throws Exception {
        long id = addOne();
        change(id, "{\"stopped\":true}").andExpect(status().isOk()).andExpect(jsonPath("$.stoppedAt").isString());
        var result = mvc.perform(get("/plans/{id}", id).with(user(Long.toString(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.studyDays[1]").value(4)).andReturn();
        String stopped = json.readTree(result.getResponse().getContentAsByteArray()).get("stoppedAt").asString();
        change(id, "{\"title\":\"자료구조\",\"studyDays\":null}")
                .andExpect(jsonPath("$.stoppedAt").value(stopped)).andExpect(jsonPath("$.title").value("자료구조"))
                .andExpect(jsonPath("$.studyDays").doesNotExist()).andExpect(jsonPath("$.credits").value(3));
        change(id, "{\"stopped\":false}").andExpect(status().isOk()).andExpect(jsonPath("$.stoppedAt").doesNotExist());
        change(id, "{\"credits\":null}").andExpect(status().isBadRequest());
    }
}
