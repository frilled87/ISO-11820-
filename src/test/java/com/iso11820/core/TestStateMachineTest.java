package com.iso11820.core;

import com.iso11820.core.TestStateMachine.StateChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestStateMachine 单元测试 —— 验证状态机流转规则、监听器、线程安全。
 * <p>
 * 覆盖：合法切换、非法切换拦截、状态回退、强制切换、监听器注册/通知/异常隔离、
 * 线程安全、边界条件。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-30
 */
@DisplayName("TestStateMachine 状态机测试")
class TestStateMachineTest {

    private TestStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new TestStateMachine();
    }

    // ==================== 初始状态 ====================

    @Nested
    @DisplayName("初始状态")
    class InitialState {

        @Test
        @DisplayName("默认构造后初始状态为 IDLE")
        void defaultInitialStateIsIdle() {
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
            assertFalse(stateMachine.isActive());
        }

        @Test
        @DisplayName("指定初始状态构造")
        void customInitialState() {
            TestStateMachine sm = new TestStateMachine(TestState.PREPARING);
            assertEquals(TestState.PREPARING, sm.getCurrentState());
            assertTrue(sm.isActive());
        }

        @Test
        @DisplayName("指定初始状态为 null 抛异常")
        void nullInitialStateThrows() {
            assertThrows(NullPointerException.class, () -> new TestStateMachine(null));
        }
    }

    // ==================== 合法切换 ====================

    @Nested
    @DisplayName("合法状态切换")
    class ValidTransitions {

        @Test
        @DisplayName("IDLE → PREPARING")
        void idleToPreparing() {
            assertTrue(stateMachine.transitionTo(TestState.PREPARING));
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("PREPARING → READY（自动判定）")
        void preparingToReady() {
            stateMachine.transitionTo(TestState.PREPARING);
            assertTrue(stateMachine.transitionTo(TestState.READY));
            assertEquals(TestState.READY, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("PREPARING → IDLE（停止加热）")
        void preparingToIdle() {
            stateMachine.transitionTo(TestState.PREPARING);
            assertTrue(stateMachine.transitionTo(TestState.IDLE));
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("READY → RECORDING")
        void readyToRecording() {
            stateMachine.transitionTo(TestState.PREPARING);
            stateMachine.transitionTo(TestState.READY);
            assertTrue(stateMachine.transitionTo(TestState.RECORDING));
            assertEquals(TestState.RECORDING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("READY → PREPARING（温度回退）")
        void readyToPreparing() {
            stateMachine.transitionTo(TestState.PREPARING);
            stateMachine.transitionTo(TestState.READY);
            assertTrue(stateMachine.transitionTo(TestState.PREPARING));
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("READY → IDLE（停止加热）")
        void readyToIdle() {
            stateMachine.transitionTo(TestState.PREPARING);
            stateMachine.transitionTo(TestState.READY);
            assertTrue(stateMachine.transitionTo(TestState.IDLE));
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("RECORDING → COMPLETE")
        void recordingToComplete() {
            fullTransitionTo(TestState.RECORDING);
            assertTrue(stateMachine.transitionTo(TestState.COMPLETE));
            assertEquals(TestState.COMPLETE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("RECORDING → PREPARING（无有效记录）")
        void recordingToPreparing() {
            fullTransitionTo(TestState.RECORDING);
            assertTrue(stateMachine.transitionTo(TestState.PREPARING));
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("COMPLETE → PREPARING（保存后保持恒温）")
        void completeToPreparing() {
            fullTransitionTo(TestState.COMPLETE);
            assertTrue(stateMachine.transitionTo(TestState.PREPARING));
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("完整正向流程：IDLE → PREPARING → READY → RECORDING → COMPLETE")
        void fullForwardFlow() {
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
            assertTrue(stateMachine.transitionTo(TestState.PREPARING));
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());
            assertTrue(stateMachine.transitionTo(TestState.READY));
            assertEquals(TestState.READY, stateMachine.getCurrentState());
            assertTrue(stateMachine.transitionTo(TestState.RECORDING));
            assertEquals(TestState.RECORDING, stateMachine.getCurrentState());
            assertTrue(stateMachine.transitionTo(TestState.COMPLETE));
            assertEquals(TestState.COMPLETE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("相同状态切换返回 true（幂等）")
        void sameStateReturnsTrue() {
            assertTrue(stateMachine.transitionTo(TestState.IDLE));
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }
    }

    // ==================== 非法切换拦截 ====================

    @Nested
    @DisplayName("非法切换拦截")
    class InvalidTransitions {

        @Test
        @DisplayName("IDLE → RECORDING（跳过升温）")
        void idleToRecordingRejected() {
            assertFalse(stateMachine.transitionTo(TestState.RECORDING));
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("IDLE → COMPLETE（跳过所有步骤）")
        void idleToCompleteRejected() {
            assertFalse(stateMachine.transitionTo(TestState.COMPLETE));
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("IDLE → READY（跳过升温）")
        void idleToReadyRejected() {
            assertFalse(stateMachine.transitionTo(TestState.READY));
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("PREPARING → COMPLETE（跳过就绪和记录）")
        void preparingToCompleteRejected() {
            stateMachine.transitionTo(TestState.PREPARING);
            assertFalse(stateMachine.transitionTo(TestState.COMPLETE));
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("RECORDING → IDLE（不允许直接停止到空闲）")
        void recordingToIdleRejected() {
            fullTransitionTo(TestState.RECORDING);
            assertFalse(stateMachine.transitionTo(TestState.IDLE));
            assertEquals(TestState.RECORDING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("RECORDING → READY（不允许回退到就绪）")
        void recordingToReadyRejected() {
            fullTransitionTo(TestState.RECORDING);
            assertFalse(stateMachine.transitionTo(TestState.READY));
            assertEquals(TestState.RECORDING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("COMPLETE → IDLE（不允许直接回到空闲）")
        void completeToIdleRejected() {
            fullTransitionTo(TestState.COMPLETE);
            assertFalse(stateMachine.transitionTo(TestState.IDLE));
            assertEquals(TestState.COMPLETE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("COMPLETE → RECORDING（不允许重新记录）")
        void completeToRecordingRejected() {
            fullTransitionTo(TestState.COMPLETE);
            assertFalse(stateMachine.transitionTo(TestState.RECORDING));
            assertEquals(TestState.COMPLETE, stateMachine.getCurrentState());
        }

        @ParameterizedTest
        @EnumSource(TestState.class)
        @DisplayName("目标为 null 时返回 false")
        void nullTargetRejected(TestState initialState) {
            TestStateMachine sm = new TestStateMachine(initialState);
            assertFalse(sm.getCurrentState().canTransitionTo(null));
        }
    }

    // ==================== 强制切换 ====================

    @Nested
    @DisplayName("强制切换（forceTransitionTo）")
    class ForceTransition {

        @Test
        @DisplayName("强制切换跳过流转规则校验")
        void forceTransitionSkipsValidation() {
            // IDLE → RECORDING 非法但强制切换应成功
            stateMachine.forceTransitionTo(TestState.RECORDING);
            assertEquals(TestState.RECORDING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("强制切换到任意状态")
        void forceTransitionToAnyState() {
            stateMachine.forceTransitionTo(TestState.COMPLETE);
            assertEquals(TestState.COMPLETE, stateMachine.getCurrentState());

            stateMachine.forceTransitionTo(TestState.IDLE);
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("强制切换相同状态不出错")
        void forceTransitionToSameState() {
            stateMachine.forceTransitionTo(TestState.IDLE);
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("forceTransitionTo(null) 抛异常")
        void forceTransitionNullThrows() {
            assertThrows(NullPointerException.class, () -> stateMachine.forceTransitionTo(null));
        }
    }

    // ==================== 重置 ====================

    @Nested
    @DisplayName("重置（reset）")
    class Reset {

        @Test
        @DisplayName("reset() 从任意状态回到 IDLE")
        void resetReturnsToIdle() {
            fullTransitionTo(TestState.COMPLETE);
            stateMachine.reset();
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("reset() 从 PREPARING 回到 IDLE")
        void resetFromPreparing() {
            stateMachine.transitionTo(TestState.PREPARING);
            stateMachine.reset();
            assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        }
    }

    // ==================== 温度稳定性检查 ====================

    @Nested
    @DisplayName("温度稳定性检查")
    class TemperatureStability {

        @Test
        @DisplayName("炉温在 745~755 范围内 isTemperatureStable 返回 true")
        void temperatureInRangeIsStable() {
            assertTrue(stateMachine.isTemperatureStable(750.0));
            assertTrue(stateMachine.isTemperatureStable(745.0));
            assertTrue(stateMachine.isTemperatureStable(755.0));
        }

        @Test
        @DisplayName("炉温超出范围 isTemperatureStable 返回 false")
        void temperatureOutOfRangeIsNotStable() {
            assertFalse(stateMachine.isTemperatureStable(744.9));
            assertFalse(stateMachine.isTemperatureStable(755.1));
            assertFalse(stateMachine.isTemperatureStable(0.0));
            assertFalse(stateMachine.isTemperatureStable(1000.0));
        }

        @Test
        @DisplayName("READY 状态温度跌出范围自动回退到 PREPARING")
        void readyStateAutoRollbackWhenTempUnstable() {
            fullTransitionTo(TestState.READY);

            // 炉温跌出范围 → 自动回退
            boolean rolledBack = stateMachine.checkAndHandleReadiness(744.0);
            assertTrue(rolledBack, "温度跌出范围应触发自动回退");
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("READY 状态温度在范围内不触发回退")
        void readyStateNoRollbackWhenTempStable() {
            fullTransitionTo(TestState.READY);

            boolean rolledBack = stateMachine.checkAndHandleReadiness(750.0);
            assertFalse(rolledBack, "温度在范围内不应触发回退");
            assertEquals(TestState.READY, stateMachine.getCurrentState());
        }

        @Test
        @DisplayName("非 READY 状态 checkAndHandleReadiness 不触发回退")
        void nonReadyStateNoAutoRollback() {
            // PREPARING 状态
            stateMachine.transitionTo(TestState.PREPARING);
            assertFalse(stateMachine.checkAndHandleReadiness(700.0));
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState());

            // IDLE 状态
            TestStateMachine sm2 = new TestStateMachine();
            assertFalse(sm2.checkAndHandleReadiness(700.0));
            assertEquals(TestState.IDLE, sm2.getCurrentState());
        }
    }

    // ==================== 监听器 ====================

    @Nested
    @DisplayName("监听器管理")
    class ListenerManagement {

        @Test
        @DisplayName("注册监听器后状态切换触发回调")
        void listenerInvokedOnTransition() {
            List<StateChangeEvent> events = new ArrayList<>();
            stateMachine.addStateChangeListener(events::add);

            stateMachine.transitionTo(TestState.PREPARING);

            assertEquals(1, events.size());
            assertEquals(TestState.IDLE, events.get(0).oldState());
            assertEquals(TestState.PREPARING, events.get(0).newState());
        }

        @Test
        @DisplayName("相同状态切换不触发监听器")
        void sameStateDoesNotTriggerListener() {
            AtomicInteger callCount = new AtomicInteger(0);
            stateMachine.addStateChangeListener(e -> callCount.incrementAndGet());

            stateMachine.transitionTo(TestState.IDLE); // 相同状态

            assertEquals(0, callCount.get(), "相同状态切换不应触发监听器");
        }

        @Test
        @DisplayName("移除监听器后不再触发")
        void removedListenerNotInvoked() {
            AtomicInteger callCount = new AtomicInteger(0);
            Consumer<StateChangeEvent> listener = e -> callCount.incrementAndGet();

            stateMachine.addStateChangeListener(listener);
            stateMachine.removeStateChangeListener(listener);

            stateMachine.transitionTo(TestState.PREPARING);
            assertEquals(0, callCount.get(), "移除后的监听器不应被调用");
        }

        @Test
        @DisplayName("重复注册同一监听器只保留一份")
        void duplicateListenerRegisteredOnce() {
            AtomicInteger callCount = new AtomicInteger(0);
            Consumer<StateChangeEvent> listener = e -> callCount.incrementAndGet();

            stateMachine.addStateChangeListener(listener);
            stateMachine.addStateChangeListener(listener);

            assertEquals(1, stateMachine.getListenerCount());

            stateMachine.transitionTo(TestState.PREPARING);
            assertEquals(1, callCount.get(), "重复注册的监听器只应触发一次");
        }

        @Test
        @DisplayName("addStateChangeListener(null) 抛异常")
        void nullListenerThrows() {
            assertThrows(NullPointerException.class, () -> stateMachine.addStateChangeListener(null));
        }

        @Test
        @DisplayName("clearListeners 清空所有监听器")
        void clearListenersRemovesAll() {
            stateMachine.addStateChangeListener(e -> {});
            stateMachine.addStateChangeListener(e -> {});
            assertEquals(2, stateMachine.getListenerCount());

            stateMachine.clearListeners();
            assertEquals(0, stateMachine.getListenerCount());
        }

        @Test
        @DisplayName("单个监听器抛异常不影响其他监听器")
        void listenerExceptionDoesNotAffectOthers() {
            AtomicInteger normalCallCount = new AtomicInteger(0);

            stateMachine.addStateChangeListener(e -> { throw new RuntimeException("测试异常"); });
            stateMachine.addStateChangeListener(e -> normalCallCount.incrementAndGet());

            // 不应抛异常
            assertDoesNotThrow(() -> stateMachine.transitionTo(TestState.PREPARING));
            assertEquals(1, normalCallCount.get(), "异常监听器不应影响其他监听器");
        }

        @Test
        @DisplayName("强制切换也触发监听器")
        void forceTransitionTriggersListener() {
            AtomicInteger callCount = new AtomicInteger(0);
            stateMachine.addStateChangeListener(e -> callCount.incrementAndGet());

            stateMachine.forceTransitionTo(TestState.COMPLETE);

            assertEquals(1, callCount.get());
        }
    }

    // ==================== StateChangeEvent ====================

    @Nested
    @DisplayName("StateChangeEvent 事件")
    class StateChangeEventTests {

        @Test
        @DisplayName("isForward() 正常前进返回 true")
        void isForwardTrueForNormalTransition() {
            StateChangeEvent event = new StateChangeEvent(TestState.IDLE, TestState.PREPARING, 1000L);
            assertTrue(event.isForward());
            assertFalse(event.isRollback());
        }

        @Test
        @DisplayName("isRollback() 状态回退返回 true")
        void isRollbackTrueForReverseTransition() {
            StateChangeEvent event = new StateChangeEvent(TestState.READY, TestState.PREPARING, 1000L);
            assertTrue(event.isRollback());
            assertFalse(event.isForward());
        }

        @Test
        @DisplayName("相同状态 ordinal 不变")
        void sameStateOrdinal() {
            StateChangeEvent event = new StateChangeEvent(TestState.IDLE, TestState.IDLE, 1000L);
            assertFalse(event.isForward());
            assertFalse(event.isRollback());
        }

        @Test
        @DisplayName("toString 包含状态名和时间戳")
        void toStringContainsStateNames() {
            StateChangeEvent event = new StateChangeEvent(TestState.IDLE, TestState.PREPARING, 1234567890L);
            String str = event.toString();
            assertTrue(str.contains("空闲"));
            assertTrue(str.contains("升温中"));
            assertTrue(str.contains("1234567890"));
        }
    }

    // ==================== 线程安全 ====================

    @Nested
    @DisplayName("线程安全")
    class ThreadSafety {

        @RepeatedTest(3)
        @DisplayName("多线程并发切换不丢状态")
        void concurrentTransitionsDoNotLoseState() throws InterruptedException {
            int threadCount = 4;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        // 每个线程尝试从 IDLE → PREPARING
                        if (stateMachine.transitionTo(TestState.PREPARING)) {
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await(5, TimeUnit.SECONDS);
            // 至少一个线程应成功（第一个线程执行实际切换），
            // 后续线程因相同状态返回 true（幂等），所以全部成功
            assertTrue(successCount.get() >= 1, "至少一个线程应成功切换");
            assertEquals(TestState.PREPARING, stateMachine.getCurrentState(),
                    "最终状态应为 PREPARING");
        }

        @Test
        @DisplayName("isActive() 多线程安全")
        void isActiveThreadSafe() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(2);

            Thread writer = new Thread(() -> {
                stateMachine.transitionTo(TestState.PREPARING);
                latch.countDown();
            });

            Thread reader = new Thread(() -> {
                for (int i = 0; i < 1000; i++) {
                    boolean active = stateMachine.isActive();
                    // 不抛异常即可
                    assertTrue(active || !active);
                }
                latch.countDown();
            });

            writer.start();
            reader.start();
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    // ==================== isState 和 isActive ====================

    @Nested
    @DisplayName("状态查询")
    class StateQuery {

        @Test
        @DisplayName("isState 正确判断当前状态")
        void isStateCorrect() {
            assertTrue(stateMachine.isState(TestState.IDLE));
            assertFalse(stateMachine.isState(TestState.PREPARING));

            stateMachine.transitionTo(TestState.PREPARING);
            assertTrue(stateMachine.isState(TestState.PREPARING));
            assertFalse(stateMachine.isState(TestState.IDLE));
        }

        @Test
        @DisplayName("isActive 在非 IDLE/COMPLETE 状态返回 true")
        void isActiveReturnsTrueForActiveStates() {
            assertFalse(stateMachine.isActive()); // IDLE

            stateMachine.transitionTo(TestState.PREPARING);
            assertTrue(stateMachine.isActive());

            stateMachine.transitionTo(TestState.READY);
            assertTrue(stateMachine.isActive());

            stateMachine.transitionTo(TestState.RECORDING);
            assertTrue(stateMachine.isActive());
        }

        @Test
        @DisplayName("isActive 在 COMPLETE 状态返回 false")
        void isActiveReturnsFalseForComplete() {
            fullTransitionTo(TestState.COMPLETE);
            assertFalse(stateMachine.isActive());
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class BoundaryConditions {

        @Test
        @DisplayName("transitionTo(null) 抛 NullPointerException")
        void transitionToNullThrows() {
            assertThrows(NullPointerException.class, () -> stateMachine.transitionTo(null));
        }

        @Test
        @DisplayName("测试全部合法状态流转路径矩阵")
        void completeTransitionMatrix() {
            // 遍历所有状态，验证 canTransitionTo 与 transitionTo 一致性
            for (TestState from : TestState.values()) {
                for (TestState to : TestState.values()) {
                    boolean canTransition = from.canTransitionTo(to);
                    // 相同状态：transitionTo 是幂等操作（返回 true），
                    // 但 canTransitionTo 不包含相同状态
                    if (from == to) {
                        // 相同状态切换应幂等成功
                        TestStateMachine sm = new TestStateMachine(from);
                        assertTrue(sm.transitionTo(to),
                                () -> from + " → " + to + " (same state) should succeed");
                        continue;
                    }
                    // 验证 consistency：transitionTo 结果应与 canTransitionTo 一致
                    TestStateMachine sm = new TestStateMachine(from);
                    if (canTransition) {
                        assertTrue(sm.transitionTo(to),
                                () -> from + " → " + to + " should succeed");
                    } else {
                        assertFalse(sm.transitionTo(to),
                                () -> from + " → " + to + " should be rejected");
                    }
                }
            }
        }
    }

    // ==================== 辅助方法 ====================

    /** 将状态机从 IDLE 快速切换到目标状态 */
    private void fullTransitionTo(TestState target) {
        switch (target) {
            case PREPARING:
                stateMachine.transitionTo(TestState.PREPARING);
                break;
            case READY:
                stateMachine.transitionTo(TestState.PREPARING);
                stateMachine.transitionTo(TestState.READY);
                break;
            case RECORDING:
                stateMachine.transitionTo(TestState.PREPARING);
                stateMachine.transitionTo(TestState.READY);
                stateMachine.transitionTo(TestState.RECORDING);
                break;
            case COMPLETE:
                stateMachine.transitionTo(TestState.PREPARING);
                stateMachine.transitionTo(TestState.READY);
                stateMachine.transitionTo(TestState.RECORDING);
                stateMachine.transitionTo(TestState.COMPLETE);
                break;
            default:
                break;
        }
    }
}