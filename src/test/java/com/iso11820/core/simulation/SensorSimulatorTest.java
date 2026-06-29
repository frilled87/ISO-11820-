package com.iso11820.core.simulation;

import com.iso11820.core.SensorData;
import com.iso11820.core.TestState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensorSimulator 单元测试 —— 验证各阶段算法正确性。
 * <p>
 * 使用固定随机种子（seed=42）确保测试可重现。
 * </p>
 */
@DisplayName("SensorSimulator 仿真引擎测试")
class SensorSimulatorTest {

    /** 固定种子，确保可重现 */
    private static final long FIXED_SEED = 42L;

    private SensorSimulator simulator;

    @BeforeEach
    void setUp() {
        // 使用默认参数 + 固定种子
        simulator = new SensorSimulator(
                750.0,   // targetTemp
                40.0,    // heatingRatePerSecond
                0.5,     // tempFluctuation
                3.0,     // stableThreshold
                25.0,    // initialTemp
                FIXED_SEED
        );
    }

    // ==================== 初始状态测试 ====================

    @Nested
    @DisplayName("初始状态")
    class InitialState {

        @Test
        @DisplayName("构造后所有通道为初始温度 25°C")
        void allChannelsAtInitialTemp() {
            SensorData data = simulator.getCurrentData();
            assertEquals(25.0, data.getTf1(), 0.05, "炉温1 应为初始温度");
            assertEquals(25.0, data.getTf2(), 0.05, "炉温2 应为初始温度");
            assertEquals(25.0, data.getTs(), 0.05, "表面温 应为初始温度");
            assertEquals(25.0, data.getTc(), 0.05, "中心温 应为初始温度");
            assertEquals(25.0, data.gettCal(), 0.5, "校准温 应接近初始温度");
        }

        @Test
        @DisplayName("初始状态为 IDLE 时 isStable() 返回 false")
        void isStableFalseInitially() {
            assertFalse(simulator.isStable());
            assertEquals(0, simulator.getStableCounter());
        }
    }

    // ==================== 升温阶段测试 ====================

    @Nested
    @DisplayName("升温阶段（PREPARING + TF1 < 747°C）")
    class HeatingPhase {

        @Test
        @DisplayName("每迭代一次炉温应显著上升（约 +32°C）")
        void temperatureIncreasesEachIteration() {
            simulator.setCurrentState(TestState.PREPARING);

            SensorData before = simulator.update();
            SensorData after = simulator.update();

            double delta = after.getTf1() - before.getTf1();
            // 升温速率 40°C/s × 0.8s = 32°C，加上噪声在 ±0.5°C 范围内
            assertTrue(delta > 30.0,
                    () -> "炉温1 应上升约 32°C，实际: " + String.format("%.1f", delta));
            assertTrue(delta < 34.0,
                    () -> "炉温1 上升不应超过 34°C，实际: " + String.format("%.1f", delta));
        }

        @Test
        @DisplayName("升温阶段表面温 ≈ 炉温1 × 0.3")
        void surfaceTempFollowsFurnaceTemp() {
            simulator.setCurrentState(TestState.PREPARING);

            SensorData data = simulator.update();
            // 已迭代一次，约 25 + 32 = 57°C
            double expectedTs = data.getTf1() * 0.3;
            // 噪声 ±0.5°C
            assertEquals(expectedTs, data.getTs(), 0.6,
                    () -> "表面温应 ≈ 炉温1 × 0.3，炉温1=" + String.format("%.1f", data.getTf1()));
        }

        @Test
        @DisplayName("升温阶段中心温 ≈ 炉温1 × 0.25")
        void centerTempFollowsFurnaceTemp() {
            simulator.setCurrentState(TestState.PREPARING);

            SensorData data = simulator.update();
            double expectedTc = data.getTf1() * 0.25;
            assertEquals(expectedTc, data.getTc(), 0.6,
                    () -> "中心温应 ≈ 炉温1 × 0.25，炉温1=" + String.format("%.1f", data.getTf1()));
        }

        @Test
        @DisplayName("升温阶段炉温1和炉温2独立噪声，两者不完全相等")
        void tf1AndTf2HaveIndependentNoise() {
            simulator.setCurrentState(TestState.PREPARING);

            // 从初始温度 25°C 开始，多次迭代检查 TF1 和 TF2 的差异
            boolean foundDifference = false;
            for (int i = 0; i < 5; i++) {
                SensorData data = simulator.update();
                if (Math.abs(data.getTf1() - data.getTf2()) > 0.01) {
                    foundDifference = true;
                    break;
                }
            }
            assertTrue(foundDifference, "炉温1 和 炉温2 应有独立噪声，多次迭代后应出现差异");
        }

        @Test
        @DisplayName("升温阶段稳定计数器始终为 0")
        void stableCounterZeroDuringHeating() {
            simulator.setCurrentState(TestState.PREPARING);

            for (int i = 0; i < 5; i++) {
                simulator.update();
                assertEquals(0, simulator.getStableCounter(),
                        "升温阶段稳定计数器应保持为 0");
            }
        }
    }

    // ==================== 稳定阶段测试 ====================

    @Nested
    @DisplayName("稳定阶段（PREPARING + TF1 >= 747°C）")
    class StablePhase {

        @Test
        @DisplayName("炉温达到 747°C 后钳位到 750°C ± 噪声")
        void temperatureClampedNearTarget() {
            simulator.setCurrentState(TestState.PREPARING);

            // 从 25°C 开始，每次约 +32°C，需要约 23 次迭代达到 747°C
            SensorData data = null;
            for (int i = 0; i < 30; i++) {
                data = simulator.update();
            }

            // 此时应已进入稳定阶段
            assertTrue(simulator.hasReachedStableThreshold(),
                    "30 次迭代后应达到稳定门槛");
            // 炉温1 应钳位在 750°C ± 0.5°C
            final double finalTf1 = data.getTf1();
            assertEquals(750.0, finalTf1, 0.5,
                    () -> "炉温1 应钳位到 750°C，实际: " + String.format("%.1f", finalTf1));
            final double finalTf2 = data.getTf2();
            assertEquals(750.0, finalTf2, 0.5,
                    () -> "炉温2 应钳位到 750°C，实际: " + String.format("%.1f", finalTf2));
        }

        @Test
        @DisplayName("稳定计数器连续累加，超过 3 次后 isStable() 返回 true")
        void stableCounterIncrementsAndIsStable() {
            simulator.setCurrentState(TestState.PREPARING);

            // 快速升温到稳定阶段
            for (int i = 0; i < 25; i++) {
                simulator.update();
            }

            // 此时应已在稳定阶段，计数器开始累加
            assertTrue(simulator.hasReachedStableThreshold());

            // 再迭代几次让计数器超过阈值
            SensorData data = null;
            for (int i = 0; i < 5; i++) {
                data = simulator.update();
            }

            assertTrue(simulator.getStableCounter() > SensorSimulator.STABLE_COUNT_THRESHOLD,
                    () -> "稳定计数器应超过阈值 3，实际: " + simulator.getStableCounter());

            // 炉温在 745~755 范围内 → isStable() 应返回 true
            if (data.getTf1() >= 745.0 && data.getTf1() <= 755.0) {
                assertTrue(simulator.isStable(),
                        "炉温在稳定范围内且计数器超过阈值，isStable() 应为 true");
            }
        }
    }

    // ==================== 记录阶段测试 ====================

    @Nested
    @DisplayName("记录阶段（RECORDING）")
    class RecordingPhase {

        @Test
        @DisplayName("记录阶段表面温向 炉温1×0.95 指数逼近")
        void surfaceTempApproachesTarget() {
            // 先快速升温到稳定阶段
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 30; i++) {
                simulator.update();
            }

            // 切换到记录阶段
            simulator.setCurrentState(TestState.RECORDING);

            SensorData first = simulator.update();
            double tsBefore = first.getTs();
            double tf1 = first.getTf1();
            double surfaceTarget = Math.min(tf1 * 0.95, 800.0);

            // 再迭代一次
            SensorData second = simulator.update();
            double tsAfter = second.getTs();

            // 表面温应向上（向目标）移动
            if (tsBefore < surfaceTarget) {
                assertTrue(tsAfter > tsBefore,
                        () -> String.format("表面温应向目标 %.1f 上升，before=%.1f, after=%.1f",
                                surfaceTarget, tsBefore, tsAfter));
            }

            // 增量约 = (surfaceTarget - tsBefore) × 0.02 ± 噪声
            double expectedDelta = (surfaceTarget - tsBefore) * 0.02;
            double actualDelta = tsAfter - tsBefore;
            assertEquals(expectedDelta, actualDelta, 0.6,
                    () -> String.format("表面温增量应约 %.2f，实际 %.2f", expectedDelta, actualDelta));
        }

        @Test
        @DisplayName("记录阶段中心温向 炉温1×0.85 指数逼近，比表面温更慢")
        void centerTempApproachesTargetSlower() {
            // 先快速升温到稳定阶段
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 30; i++) {
                simulator.update();
            }

            simulator.setCurrentState(TestState.RECORDING);

            SensorData first = simulator.update();
            double tcBefore = first.getTc();
            double tf1 = first.getTf1();
            double centerTarget = Math.min(tf1 * 0.85, 750.0);

            SensorData second = simulator.update();
            double tcAfter = second.getTc();

            if (tcBefore < centerTarget) {
                assertTrue(tcAfter > tcBefore,
                        "中心温应向目标上升");
            }

            // 系数 0.01，比表面温 0.02 慢一半
            double expectedDelta = (centerTarget - tcBefore) * 0.01;
            double actualDelta = tcAfter - tcBefore;
            assertEquals(expectedDelta, actualDelta, 0.6,
                    () -> String.format("中心温增量应约 %.2f，实际 %.2f", expectedDelta, actualDelta));
        }

        @Test
        @DisplayName("记录阶段炉温1和炉温2维持稳定（750°C ± 噪声）")
        void furnaceTempsRemainStable() {
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 30; i++) {
                simulator.update();
            }

            simulator.setCurrentState(TestState.RECORDING);

            for (int i = 0; i < 10; i++) {
                SensorData data = simulator.update();
                assertEquals(750.0, data.getTf1(), 0.5,
                        "记录阶段炉温1 应维持 750°C ± 0.5°C");
                assertEquals(750.0, data.getTf2(), 0.5,
                        "记录阶段炉温2 应维持 750°C ± 0.5°C");
            }
        }
    }

    // ==================== 降温阶段测试 ====================

    @Nested
    @DisplayName("降温阶段（IDLE + 炉温 > 初始温度）")
    class CoolingPhase {

        @Test
        @DisplayName("IDLE 状态下炉温应下降")
        void temperatureDecreasesInIdle() {
            // 先升温到较高温度
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 10; i++) {
                simulator.update();
            }

            // 切换到 IDLE（停止加热）
            simulator.setCurrentState(TestState.IDLE);

            SensorData before = simulator.update();
            SensorData after = simulator.update();

            assertTrue(after.getTf1() < before.getTf1(),
                    () -> "降温阶段炉温1 应下降: before="
                            + String.format("%.1f", before.getTf1())
                            + ", after=" + String.format("%.1f", after.getTf1()));
        }

        @Test
        @DisplayName("降温阶段每次约降 0.5°C")
        void coolingRateIsCorrect() {
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 10; i++) {
                simulator.update();
            }

            simulator.setCurrentState(TestState.IDLE);

            SensorData before = simulator.update();
            SensorData after = simulator.update();

            double delta = before.getTf1() - after.getTf1();
            // 降温速率 0.5°C/次 ± 噪声 × 0.1
            assertTrue(delta > 0.3,
                    () -> "降温速率应约 0.5°C/次，实际: " + String.format("%.2f", delta));
            assertTrue(delta < 0.7,
                    () -> "降温速率不应超过 0.7°C/次，实际: " + String.format("%.2f", delta));
        }

        @Test
        @DisplayName("温度降至初始温度以下后钳位不继续下降")
        void temperatureClampedAtInitial() {
            // 设置初始温度较高，方便测试
            simulator = new SensorSimulator(750.0, 40.0, 0.5, 3.0, 100.0, FIXED_SEED);

            // 先升温
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 5; i++) {
                simulator.update();
            }

            // 切换到 IDLE 降温
            simulator.setCurrentState(TestState.IDLE);

            // 降温足够多次
            for (int i = 0; i < 500; i++) {
                simulator.update();
            }

            SensorData data = simulator.getCurrentData();
            // 温度不应低于初始温度 100°C
            assertTrue(data.getTf1() >= 100.0,
                    () -> "炉温1 不应低于初始温度 100°C，实际: " + String.format("%.1f", data.getTf1()));
            assertTrue(data.getTf2() >= 100.0,
                    () -> "炉温2 不应低于初始温度 100°C，实际: " + String.format("%.1f", data.getTf2()));
        }
    }

    // ==================== 线程安全测试 ====================

    @Nested
    @DisplayName("线程安全")
    class ThreadSafety {

        @Test
        @DisplayName("update() 返回的 SensorData 是副本，外部修改不影响内部状态")
        void returnedDataIsCopy() {
            simulator.setCurrentState(TestState.PREPARING);
            SensorData data = simulator.update();

            // 修改返回的副本
            data.setTf1(999.0);

            // 内部状态不应被修改
            SensorData internal = simulator.getCurrentData();
            assertNotEquals(999.0, internal.getTf1(), 0.01,
                    "返回的副本修改不应影响内部状态");
        }

        @RepeatedTest(3)
        @DisplayName("多线程并发调用 update() 不抛出异常")
        void concurrentUpdatesDoNotThrow() throws InterruptedException {
            simulator.setCurrentState(TestState.PREPARING);

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 50; i++) simulator.update();
            });
            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 50; i++) simulator.update();
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            // 无异常即通过
            SensorData data = simulator.getCurrentData();
            assertTrue(data.getTf1() > 25.0, "多次迭代后炉温应上升");
        }
    }

    // ==================== 噪声生成测试 ====================

    @Nested
    @DisplayName("随机噪声")
    class NoiseGeneration {

        @Test
        @DisplayName("随机噪声在 [-0.5, +0.5] 范围内（默认 TempFluctuation=0.5）")
        void noiseWithinRange() {
            simulator.setCurrentState(TestState.PREPARING);

            double minNoise = Double.MAX_VALUE;
            double maxNoise = Double.MIN_VALUE;

            // 大量采样验证噪声范围
            for (int i = 0; i < 1000; i++) {
                // 重置到初始状态避免温度钳位影响
                simulator.reset();
                simulator.setCurrentState(TestState.PREPARING);
                SensorData data = simulator.update();

                // 噪声 = 实际增量 - 理论增量（32°C）
                double noise = data.getTf1() - 25.0 - 32.0;
                minNoise = Math.min(minNoise, noise);
                maxNoise = Math.max(maxNoise, noise);
            }

            final double finalMin = minNoise;
            final double finalMax = maxNoise;
            assertTrue(finalMin >= -0.5,
                    () -> "噪声下限应 >= -0.5，实际: " + String.format("%.3f", finalMin));
            assertTrue(finalMax <= 0.5,
                    () -> "噪声上限应 <= 0.5，实际: " + String.format("%.3f", finalMax));
        }
    }

    // ==================== 状态切换测试 ====================

    @Nested
    @DisplayName("状态切换")
    class StateTransitions {

        @Test
        @DisplayName("PREPARING → RECORDING：表面温从低值跟随切换到指数逼近")
        void surfaceTempBehaviorChangesOnRecording() {
            // PREPARING 阶段升温
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 30; i++) {
                simulator.update();
            }

            // 记录 PREPARING 最后时刻的表面温
            SensorData beforeRecording = simulator.getCurrentData();
            double tsPreparing = beforeRecording.getTs();

            // 切换到 RECORDING
            simulator.setCurrentState(TestState.RECORDING);

            // 记录阶段第一次更新
            SensorData afterRecording = simulator.update();
            double tsRecording = afterRecording.getTs();

            // 记录阶段表面温应开始向炉温1×0.95 移动
            // 而 PREPARING 稳定阶段表面温 ≈ 炉温1×0.3
            double surfaceTarget = Math.min(afterRecording.getTf1() * 0.95, 800.0);
            assertTrue(tsRecording > tsPreparing,
                    () -> String.format("进入 RECORDING 后表面温应上升: %.1f → %.1f (目标: %.1f)",
                            tsPreparing, tsRecording, surfaceTarget));
        }

        @Test
        @DisplayName("IDLE → PREPARING 开始升温，稳定计数器重置")
        void heatingResetsStableCounter() {
            // 先进入稳定阶段
            simulator.setCurrentState(TestState.PREPARING);
            for (int i = 0; i < 30; i++) {
                simulator.update();
            }
            // 此时稳定计数器已超过阈值
            assertTrue(simulator.getStableCounter() > 0);

            // 切换到 IDLE（冷却）
            simulator.setCurrentState(TestState.IDLE);
            simulator.update();
            assertEquals(0, simulator.getStableCounter(),
                    "进入 IDLE 后稳定计数器应重置");
            assertFalse(simulator.isStableFlag(),
                    "进入 IDLE 后稳定标记应重置");
        }
    }
}