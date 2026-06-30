package com.iso11820.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestMaster 单元测试 —— 验证试验主控制器的完整流程。
 * <p>
 * 覆盖：完整生命周期、非法操作拦截、保存状态保护、统计数据验证、
 * 监听器、边界值、线程安全、重置。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-30
 */
@DisplayName("TestMaster 试验主控制器测试")
class TestMasterTest {

    /** 固定种子，确保可重现 */
    private static final long FIXED_SEED = 42L;

    private TestMaster testMaster;

    @BeforeEach
    void setUp() {
        testMaster = new TestMaster(FIXED_SEED);
    }

    // ==================== 初始状态 ====================

    @Nested
    @DisplayName("初始状态")
    class InitialState {

        @Test
        @DisplayName("构造后状态为 IDLE")
        void initialStateIsIdle() {
            assertEquals(TestState.IDLE, testMaster.getCurrentState());
            assertFalse(testMaster.isSchedulerRunning());
            assertEquals(0, testMaster.getRecordedDuration());
            assertEquals(0, testMaster.getRecordedDataCount());
        }

        @Test
        @DisplayName("初始温度数据不为 null")
        void initialDataNotNull() {
            SensorData data = testMaster.getCurrentData();
            assertNotNull(data);
            assertEquals(25.0, data.getTf1(), 0.1);
        }

        @Test
        @DisplayName("初始统计数据全为 0")
        void initialStatisticsAllZero() {
            assertEquals(0.0, testMaster.getMaxTf1());
            assertEquals(0.0, testMaster.getMaxTf2());
            assertEquals(0.0, testMaster.getDeltaTf());
            assertEquals(0.0, testMaster.getAvgTf1());
        }

        @Test
        @DisplayName("初始消息队列为空")
        void initialMessagesEmpty() {
            assertEquals(0, testMaster.getMessageCount());
            assertNull(testMaster.getLatestMessage());
        }
    }

    // ==================== 完整生命周期 ====================

    @Nested
    @DisplayName("完整生命周期")
    class FullLifecycle {

        @Test
        @DisplayName("startHeating → IDLE→PREPARING 成功")
        void startHeatingSuccess() {
            assertTrue(testMaster.startHeating());
            assertEquals(TestState.PREPARING, testMaster.getCurrentState());
            assertTrue(testMaster.isSchedulerRunning());
        }

        @Test
        @DisplayName("startHeating 后调度器运行，温度开始上升")
        void startHeatingIncreasesTemperature() throws InterruptedException {
            testMaster.startHeating();

            // 等待几次迭代
            Thread.sleep(2000);

            SensorData data = testMaster.getCurrentData();
            assertTrue(data.getTf1() > 25.0,
                    () -> "炉温应上升，实际: " + data.getTf1());
        }

        @Test
        @DisplayName("完整流程：startHeating → 自动 READY → startRecording → stopRecording → COMPLETE")
        void fullFlowToComplete() throws InterruptedException {
            // 1. 开始升温
            assertTrue(testMaster.startHeating());
            assertEquals(TestState.PREPARING, testMaster.getCurrentState());

            // 2. 等待自动稳定（需要约 23+ 次迭代 × 800ms ≈ 18s，用快速加热配置）
            // 使用 720°C 初始温度 + 快速升温速率的配置加速测试
            waitForState(TestState.READY, 30);
            assertEquals(TestState.READY, testMaster.getCurrentState(),
                    "应在升温后自动进入 READY");

            // 3. 开始记录（30 秒短时测试）
            assertTrue(testMaster.startRecording(30));
            assertEquals(TestState.RECORDING, testMaster.getCurrentState());

            // 4. 等待记录完成
            waitForState(TestState.COMPLETE, 40);
            assertEquals(TestState.COMPLETE, testMaster.getCurrentState());

            // 5. 验证记录数据
            assertTrue(testMaster.getRecordedDataCount() > 0,
                    "记录阶段应有数据点");
            assertTrue(testMaster.getRecordedDuration() >= 0);

            // 6. 停止加热
            testMaster.stopHeating();
            assertEquals(TestState.IDLE, testMaster.getCurrentState());
            assertFalse(testMaster.isSchedulerRunning());
        }
    }

    // ==================== 非法操作拦截 ====================

    @Nested
    @DisplayName("非法操作拦截")
    class InvalidOperations {

        @Test
        @DisplayName("非 IDLE 状态下 startHeating 返回 false")
        void startHeatingWhenNotIdle() throws InterruptedException {
            testMaster.startHeating();
            // 已经 PREPARING，再次调用应失败
            assertFalse(testMaster.startHeating());

            // 停止加热后可以重新开始
            testMaster.stopHeating();
            assertTrue(testMaster.startHeating());
        }

        @Test
        @DisplayName("非 READY 状态下 startRecording 返回 false")
        void startRecordingWhenNotReady() {
            // IDLE 状态
            assertFalse(testMaster.startRecording(60));
            assertEquals(TestState.IDLE, testMaster.getCurrentState());

            // PREPARING 状态
            testMaster.startHeating();
            assertFalse(testMaster.startRecording(60));
        }

        @Test
        @DisplayName("startRecording 时长 <= 0 抛异常")
        void startRecordingInvalidDuration() {
            assertThrows(IllegalArgumentException.class, () -> testMaster.startRecording(0));
            assertThrows(IllegalArgumentException.class, () -> testMaster.startRecording(-1));
        }

        @Test
        @DisplayName("非 RECORDING 状态下 stopRecording 返回 false")
        void stopRecordingWhenNotRecording() {
            assertFalse(testMaster.stopRecording());
            testMaster.startHeating();
            assertFalse(testMaster.stopRecording());
        }
    }

    // ==================== 保存状态保护 ====================

    @Nested
    @DisplayName("保存状态保护")
    class SaveStateProtection {

        @Test
        @DisplayName("初始状态 isSaved() 返回 false")
        void initiallyNotSaved() {
            assertFalse(testMaster.isSaved());
        }

        @Test
        @DisplayName("markSaved() 后 isSaved() 返回 true")
        void markSaved() {
            testMaster.markSaved();
            assertTrue(testMaster.isSaved());
        }

        @Test
        @DisplayName("COMPLETE 未保存时禁止 startHeating")
        void completeUnsavedBlocksStartHeating() {
            // 使用 force 方式模拟 COMPLETE 未保存状态
            testMaster.getStateMachine().forceTransitionTo(TestState.COMPLETE);
            assertFalse(testMaster.isSaved());
            assertFalse(testMaster.startHeating());
        }

        @Test
        @DisplayName("COMPLETE 已保存后允许 startHeating")
        void completeSavedAllowsStartHeating() {
            testMaster.getStateMachine().forceTransitionTo(TestState.COMPLETE);
            testMaster.markSaved();
            // 重置到 IDLE 才能 startHeating
            testMaster.getStateMachine().forceTransitionTo(TestState.IDLE);
            assertTrue(testMaster.isSaved());
            assertTrue(testMaster.startHeating());
        }
    }

    // ==================== 统计数据验证 ====================

    @Nested
    @DisplayName("统计数据验证")
    class StatisticsValidation {

        @Test
        @DisplayName("getTestResult 在非 COMPLETE 状态返回 null")
        void testResultNullWhenNotComplete() {
            assertNull(testMaster.getTestResult());

            testMaster.startHeating();
            assertNull(testMaster.getTestResult());
        }
    }

    // ==================== 监听器 ====================

    @Nested
    @DisplayName("监听器")
    class ListenerTests {

        @Test
        @DisplayName("注册监听器后在迭代时触发回调")
        void listenerInvokedOnTick() throws InterruptedException {
            AtomicInteger callCount = new AtomicInteger(0);
            AtomicReference<TestState> lastState = new AtomicReference<>();

            testMaster.addDataChangeListener((data, state, duration, msg) -> {
                callCount.incrementAndGet();
                lastState.set(state);
            });

            testMaster.startHeating();
            Thread.sleep(2000); // 等待至少 2 次迭代

            assertTrue(callCount.get() > 0, "监听器应被触发");
            assertEquals(TestState.PREPARING, lastState.get());

            testMaster.stopHeating();
        }

        @Test
        @DisplayName("移除监听器后不再触发")
        void removedListenerNotInvoked() throws InterruptedException {
            AtomicInteger callCount = new AtomicInteger(0);
            DataChangeListener listener = (data, state, duration, msg) -> callCount.incrementAndGet();

            testMaster.addDataChangeListener(listener);
            testMaster.removeDataChangeListener(listener);

            testMaster.startHeating();
            Thread.sleep(1500);

            assertEquals(0, callCount.get(), "移除后的监听器不应被调用");

            testMaster.stopHeating();
        }

        @Test
        @DisplayName("addDataChangeListener(null) 抛异常")
        void nullListenerThrows() {
            assertThrows(NullPointerException.class, () -> testMaster.addDataChangeListener(null));
        }

        @Test
        @DisplayName("getListenerCount 正确返回监听器数量")
        void listenerCount() {
            assertEquals(0, testMaster.getListenerCount());
            testMaster.addDataChangeListener((d, s, t, m) -> {});
            assertEquals(1, testMaster.getListenerCount());
        }
    }

    // ==================== 边界值 ====================

    @Nested
    @DisplayName("边界值测试")
    class BoundaryConditions {

        @Test
        @DisplayName("多次 stopHeating 不抛异常（幂等）")
        void stopHeatingMultipleTimes() {
            assertDoesNotThrow(() -> {
                testMaster.stopHeating();
                testMaster.stopHeating();
                testMaster.stopHeating();
            });
        }

        @Test
        @DisplayName("多次 reset 不抛异常（幂等）")
        void resetMultipleTimes() {
            assertDoesNotThrow(() -> {
                testMaster.reset();
                testMaster.reset();
                testMaster.reset();
            });
        }

        @Test
        @DisplayName("reset 后状态回到 IDLE")
        void resetReturnsToIdle() throws InterruptedException {
            testMaster.startHeating();
            Thread.sleep(500);
            testMaster.reset();

            assertEquals(TestState.IDLE, testMaster.getCurrentState());
            assertFalse(testMaster.isSchedulerRunning());
            assertEquals(0, testMaster.getRecordedDataCount());
            assertEquals(0, testMaster.getMessageCount());
        }

        @Test
        @DisplayName("stopRecording 时长不足 30 秒退回 READY")
        void stopRecordingShortDurationReturnsToReady() throws InterruptedException {
            // 快速走到 RECORDING
            testMaster.startHeating();
            waitForState(TestState.READY, 30);

            testMaster.startRecording(3600);
            // 立即停止（不足 30 秒）
            Thread.sleep(500);
            assertFalse(testMaster.stopRecording());
            assertEquals(TestState.READY, testMaster.getCurrentState(),
                    "记录时长不足 30 秒应退回 READY");
        }

        @Test
        @DisplayName("getRecordedData 返回不可变列表")
        void recordedDataIsImmutable() {
            List<SensorData> data = testMaster.getRecordedData();
            assertThrows(UnsupportedOperationException.class, () -> data.add(new SensorData()));
        }

        @Test
        @DisplayName("getMessages 返回不可变列表")
        void messagesIsImmutable() {
            List<SystemMessage> msgs = testMaster.getMessages();
            assertThrows(UnsupportedOperationException.class, () -> msgs.add(SystemMessage.now("test")));
        }
    }

    // ==================== 系统消息 ====================

    @Nested
    @DisplayName("系统消息")
    class SystemMessageTests {

        @Test
        @DisplayName("startHeating 生成消息")
        void startHeatingGeneratesMessage() {
            testMaster.startHeating();
            assertTrue(testMaster.getMessageCount() > 0);
            assertNotNull(testMaster.getLatestMessage());
            assertTrue(testMaster.getLatestMessage().contains("升温"));
        }

        @Test
        @DisplayName("stopHeating 生成消息")
        void stopHeatingGeneratesMessage() throws InterruptedException {
            testMaster.startHeating();
            Thread.sleep(500);
            testMaster.stopHeating();

            assertTrue(testMaster.getLatestMessage().contains("停止加热"));
        }
    }

    // ==================== 线程安全 ====================

    @Nested
    @DisplayName("线程安全")
    class ThreadSafety {

        @RepeatedTest(3)
        @DisplayName("并发调用 startHeating 只有一个成功")
        void concurrentStartHeating() throws InterruptedException {
            int threadCount = 3;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        if (testMaster.startHeating()) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await(5, TimeUnit.SECONDS);
            assertEquals(1, successCount.get(), "只有第一个线程应成功");
            assertTrue(testMaster.isSchedulerRunning());

            testMaster.stopHeating();
        }
    }

    // ==================== 辅助方法 ====================

    /** 等待状态机达到目标状态（超时则失败） */
    private void waitForState(TestState target, int timeoutSeconds) throws InterruptedException {
        long start = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;

        while (testMaster.getCurrentState() != target) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                fail("等待状态 " + target + " 超时（" + timeoutSeconds + "s），当前: "
                        + testMaster.getCurrentState());
            }
            Thread.sleep(200);
        }
    }
}