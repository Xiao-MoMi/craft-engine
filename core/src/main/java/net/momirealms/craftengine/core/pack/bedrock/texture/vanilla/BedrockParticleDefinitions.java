package net.momirealms.craftengine.core.pack.bedrock.texture.vanilla;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

// Bedrock Samples v1.26.50.4 和 Geyser Integrated Pack 的行为数据；运行时不读取粒子模板 JSON。
// 保留原始发射、运动、染色、事件和曲线，UV 由 Java 粒子定义重新生成。
// 来源及许可证见 THIRD_PARTY_LICENSES
final class BedrockParticleDefinitions {
    static final BedrockParticleDefinition ASH_PARTICLE = createAshParticle();
    static final BedrockParticleDefinition BASIC_BUBBLE_MANUAL = createBasicBubbleManual();
    static final BedrockParticleDefinition BASIC_FLAME = createBasicFlame();
    static final BedrockParticleDefinition BASIC_SMOKE = createBasicSmoke();
    static final BedrockParticleDefinition BIOME_TINTED_LEAVES_PARTICLE = createBiomeTintedLeavesParticle();
    static final BedrockParticleDefinition BLUE_FLAME = createBlueFlame();
    static final BedrockParticleDefinition BREEZE_WIND_EXPLOSION = createBreezeWindExplosion();
    static final BedrockParticleDefinition BUBBLE_COLUMN_DOWN = createBubbleColumnDown();
    static final BedrockParticleDefinition BUBBLE_COLUMN_UP = createBubbleColumnUp();
    static final BedrockParticleDefinition CAMPFIRE_SMOKE = createCampfireSmoke();
    static final BedrockParticleDefinition CAMPFIRE_SMOKE_TALL = createCampfireSmokeTall();
    static final BedrockParticleDefinition CANDLE_FLAME = createCandleFlame();
    static final BedrockParticleDefinition CHERRY_LEAVES_PARTICLE = createCherryLeavesParticle();
    static final BedrockParticleDefinition CREAKING_HEART_TRAIL = createCreakingHeartTrail();
    static final BedrockParticleDefinition CRIMSON_SPORE_PARTICLE = createCrimsonSporeParticle();
    static final BedrockParticleDefinition CRITICAL_HIT = createCriticalHit();
    static final BedrockParticleDefinition DAMAGE_INDICATOR_PARTICLE = createDamageIndicatorParticle();
    static final BedrockParticleDefinition DOLPHIN_MOVE_PARTICLE = createDolphinMoveParticle();
    static final BedrockParticleDefinition DRAGON_BREATH_LINGERING = createDragonBreathLingering();
    static final BedrockParticleDefinition DRIPSTONE_LAVA_DRIP = createDripstoneLavaDrip();
    static final BedrockParticleDefinition DRIPSTONE_WATER_DRIP = createDripstoneWaterDrip();
    static final BedrockParticleDefinition DUST_PLUME = createDustPlume();
    static final BedrockParticleDefinition ELECTRIC_SPARK = createElectricSpark();
    static final BedrockParticleDefinition ENCHANTED_HIT_SINGLE_PARTICLE = createEnchantedHitSingleParticle();
    static final BedrockParticleDefinition ENCHANTING_TABLE_PARTICLE = createEnchantingTableParticle();
    static final BedrockParticleDefinition ENDROD = createEndrod();
    static final BedrockParticleDefinition EVOKER_SPELL = createEvokerSpell();
    static final BedrockParticleDefinition EXPLOSION_MANUAL = createExplosionManual();
    static final BedrockParticleDefinition FALLING_DUST = createFallingDust();
    static final BedrockParticleDefinition FIREFLY_PARTICLE = createFireflyParticle();
    static final BedrockParticleDefinition FLASH_PARTICLE = createFlashParticle();
    static final BedrockParticleDefinition GEYSER_BASE_PARTICLE = createGeyserBaseParticle();
    static final BedrockParticleDefinition GEYSER_PLUME_PARTICLE = createGeyserPlumeParticle();
    static final BedrockParticleDefinition GEYSER_POOF_PARTICLE = createGeyserPoofParticle();
    static final BedrockParticleDefinition GLOW = createGlow();
    static final BedrockParticleDefinition GREEN_FLAME = createGreenFlame();
    static final BedrockParticleDefinition HEART = createHeart();
    static final BedrockParticleDefinition HONEY_DRIP = createHoneyDrip();
    static final BedrockParticleDefinition INFESTED_EMITTER = createInfestedEmitter();
    static final BedrockParticleDefinition INK = createInk();
    static final BedrockParticleDefinition LANDING_HONEY_PARTICLE = createLandingHoneyParticle();
    static final BedrockParticleDefinition LANDING_LAVA_PARTICLE = createLandingLavaParticle();
    static final BedrockParticleDefinition LANDING_OBSIDIAN_TEAR_PARTICLE = createLandingObsidianTearParticle();
    static final BedrockParticleDefinition LARGE_EXPLOSION_LEVEL = createLargeExplosionLevel();
    static final BedrockParticleDefinition LAVA_DRIP = createLavaDrip();
    static final BedrockParticleDefinition LAVA_PARTICLE = createLavaParticle();
    static final BedrockParticleDefinition LLAMA_SPIT = createLlamaSpit();
    static final BedrockParticleDefinition MOB_PORTAL = createMobPortal();
    static final BedrockParticleDefinition MOBSPELL = createMobspell();
    static final BedrockParticleDefinition MYCELIUM_DUST = createMyceliumDust();
    static final BedrockParticleDefinition NAUTILUS_PARTICLE = createNautilusParticle();
    static final BedrockParticleDefinition NECTAR_DRIP = createNectarDrip();
    static final BedrockParticleDefinition NOTE = createNote();
    static final BedrockParticleDefinition NOXIOUS_GAS_PARTICLE = createNoxiousGasParticle();
    static final BedrockParticleDefinition OBSIDIAN_TEAR = createObsidianTear();
    static final BedrockParticleDefinition OMINOUS_SPAWNING_PARTICLE = createOminousSpawningParticle();
    static final BedrockParticleDefinition ORANGE_POPLAR_LEAVES_PARTICLE = createOrangePoplarLeavesParticle();
    static final BedrockParticleDefinition PALE_OAK_LEAVES_PARTICLE = createPaleOakLeavesParticle();
    static final BedrockParticleDefinition PAUSE_MOB_GROWTH = createPauseMobGrowth();
    static final BedrockParticleDefinition PORTAL_REVERSE = createPortalReverse();
    static final BedrockParticleDefinition RAID_OMEN_AMBIENT = createRaidOmenAmbient();
    static final BedrockParticleDefinition RAIN_SPLASH = createRainSplash();
    static final BedrockParticleDefinition RED_POPLAR_LEAVES_PARTICLE = createRedPoplarLeavesParticle();
    static final BedrockParticleDefinition RESET_MOB_GROWTH = createResetMobGrowth();
    static final BedrockParticleDefinition SCULK_CHARGE = createSculkCharge();
    static final BedrockParticleDefinition SCULK_CHARGE_POP = createSculkChargePop();
    static final BedrockParticleDefinition SCULK_SOUL = createSculkSoul();
    static final BedrockParticleDefinition SHRIEK = createShriek();
    static final BedrockParticleDefinition SNEEZE = createSneeze();
    static final BedrockParticleDefinition SNOWFLAKE = createSnowflake();
    static final BedrockParticleDefinition SONIC_EXPLOSION = createSonicExplosion();
    static final BedrockParticleDefinition SOUL = createSoul();
    static final BedrockParticleDefinition SPARKLER = createSparkler();
    static final BedrockParticleDefinition SPLASHPOTIONSPELL = createSplashpotionspell();
    static final BedrockParticleDefinition SPORE_BLOSSOM_AMBIENT_BLOCK_ACTOR = createSporeBlossomAmbientBlockActor();
    static final BedrockParticleDefinition SPORE_BLOSSOM_SHOWER = createSporeBlossomShower();
    static final BedrockParticleDefinition SULFUR_BUBBLE_PARTICLE = createSulfurBubbleParticle();
    static final BedrockParticleDefinition SULFUR_CUBE_PARTICLE = createSulfurCubeParticle();
    static final BedrockParticleDefinition SWEEP_ATTACK_PARTICLE = createSweepAttackParticle();
    static final BedrockParticleDefinition TOTEM = createTotem();
    static final BedrockParticleDefinition TRIAL_OMEN_EMITTER = createTrialOmenEmitter();
    static final BedrockParticleDefinition TRIAL_SPAWNER_DETECTION_PARTICLE = createTrialSpawnerDetectionParticle();
    static final BedrockParticleDefinition TRIAL_SPAWNER_DETECTION_OMINOUS_PARTICLE = createTrialSpawnerDetectionOminousParticle();
    static final BedrockParticleDefinition UNDERWATER_PARTICLE = createUnderwaterParticle();
    static final BedrockParticleDefinition VAULT_CONNECTION = createVaultConnection();
    static final BedrockParticleDefinition VIBRATION_SIGNAL = createVibrationSignal();
    static final BedrockParticleDefinition VILLAGER_ANGRY = createVillagerAngry();
    static final BedrockParticleDefinition VILLAGER_HAPPY = createVillagerHappy();
    static final BedrockParticleDefinition WARPED_SPORE_PARTICLE = createWarpedSporeParticle();
    static final BedrockParticleDefinition WATER_DRIP = createWaterDrip();
    static final BedrockParticleDefinition WATER_EVAPORATION_ACTOR = createWaterEvaporationActor();
    static final BedrockParticleDefinition WATER_EVAPORATION_BUCKET = createWaterEvaporationBucket();
    static final BedrockParticleDefinition WATER_SPLASH = createWaterSplash();
    static final BedrockParticleDefinition WATER_SPLASH_MANUAL = createWaterSplashManual();
    static final BedrockParticleDefinition WATER_WAKE = createWaterWake();
    static final BedrockParticleDefinition WAX = createWax();
    static final BedrockParticleDefinition WHITE_ASH_PARTICLE = createWhiteAshParticle();
    static final BedrockParticleDefinition WHITE_SMOKE = createWhiteSmoke();
    static final BedrockParticleDefinition WIND_CHARGED_AMBIENT = createWindChargedAmbient();
    static final BedrockParticleDefinition WIND_CHARGED_EMITTER = createWindChargedEmitter();
    static final BedrockParticleDefinition WIND_EXPLOSION = createWindExplosion();
    static final BedrockParticleDefinition WITCHSPELL = createWitchspell();
    static final BedrockParticleDefinition YELLOW_POPLAR_LEAVES_PARTICLE = createYellowPoplarLeavesParticle();

    private BedrockParticleDefinitions() {
    }

    private static BedrockParticleDefinition createAshParticle() {
        return new BedrockParticleDefinition(
                "ash.particle",
                "1.10.0",
                "geyseropt:ash",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.random = math.random(0, 1);")
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.75)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.75)
                        )),
                        entry("minecraft:particle_initial_speed", 0.2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -3, 0)),
                                entry("linear_drag_coefficient", 2)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.16, 0.16)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("interpolant", "v.random"),
                                        entry("gradient", Map.ofEntries(
                                                entry("0.0", "#FFA5A5A5"),
                                                entry("1.0", "#ff000000")
                                        ))
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBasicBubbleManual() {
        return new BedrockParticleDefinition(
                "basic_bubble_manual",
                "1.26.10",
                "minecraft:basic_bubble_particle_manual",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.05*variable.particle_random_1+0.1)*(variable.particle_random_2*0.9+0.2)", "(0.05*variable.particle_random_1+0.1)*(variable.particle_random_2*0.9+0.2)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_expire_if_not_in_blocks", List.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "2 / ((Math.Random(0.0, 1.0) * 0.8 + 0.2) * 5)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.8, 0)),
                                entry("linear_drag_coefficient", 5.25)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBasicFlame() {
        return new BedrockParticleDefinition(
                "basic_flame",
                "1.26.10",
                "minecraft:basic_flame_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 0.025),
                                entry("direction", List.of(0, 0, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.1 + variable.particle_random_1*0.1) - (0.1 * variable.particle_age)", "(0.1 + variable.particle_random_1*0.1) - (0.1 * variable.particle_age)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.6, 2.0)")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBasicSmoke() {
        return new BedrockParticleDefinition(
                "basic_smoke",
                "1.26.10",
                "minecraft:basic_smoke_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.1, 0.1)", 1, "Math.random(-0.1, 0.1)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.1, 0.1)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1*0.5", "variable.particle_random_1*0.5", "variable.particle_random_1*0.5", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 1),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.4, 1.4)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.4, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBiomeTintedLeavesParticle() {
        return new BedrockParticleDefinition(
                "biome_tinted_leaves_particle",
                "1.26.10",
                "minecraft:biome_tinted_leaves_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.fall_acc = -0.3; v.wind_acc = 10; v.curve_len = 15;")
                        )),
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 2000)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, -0.075, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("direction", "outwards")
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("v.particle_random_1 > 0.5 ? 0.1 : 0.15", "v.particle_random_1 > 0.5 ? 0.1 : 0.15")),
                                entry("facing_camera_mode", "lookat_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of("variable.color.r", "variable.color.g", "variable.color.b", 1))
                        )),
                        entry("minecraft:particle_initial_speed", List.of(0, -0.021, 0)),
                        entry("minecraft:particle_initial_spin", Map.ofEntries(
                                entry("rotation", "math.random(0, 360)"),
                                entry("rotation_rate", "math.random(-30, 30)")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 15)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("collision_radius", 0.075),
                                entry("expire_on_contact", true)
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("math.cos((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)", "v.fall_acc", "math.sin((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)")),
                                entry("rotation_acceleration", "v.particle_random_4 > 0.5 ? -5 : 5")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBlueFlame() {
        return new BedrockParticleDefinition(
                "blue_flame",
                "1.26.10",
                "minecraft:blue_flame_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 0.025),
                                entry("direction", List.of(0, 0, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.1 + variable.particle_random_1*0.1) - (0.1 * variable.particle_age)", "(0.1 + variable.particle_random_1*0.1) - (0.1 * variable.particle_age)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.6, 2.0)")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBreezeWindExplosion() {
        return new BedrockParticleDefinition(
                "breeze_wind_explosion",
                "1.26.10",
                "minecraft:breeze_wind_explosion_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.4)
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 120),
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("Math.random(-4, 4)", "Math.random(-4, 4)", "Math.random(-4, 4)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(1, 1)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random( 0.8, 0.9 )")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBubbleColumnDown() {
        return new BedrockParticleDefinition(
                "bubble_column_down",
                "1.26.10",
                "minecraft:bubble_column_down_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 1000)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of(0, -1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_expire_if_not_in_blocks", List.of("minecraft:water", "minecraft:flowing_water", "minecraft:bubble_column")),
                        entry("minecraft:particle_initial_speed", 1),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.angle = variable.particle_random_1 * 360 + variable.particle_age * 180;"),
                                entry("per_render_expression", "variable.size = (0.05 * variable.particle_random_3+0.1)*(variable.particle_random_4*0.9+0.2);")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 10)
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("Math.cos(variable.angle)*3", -1.3, "Math.sin(variable.angle)*3")),
                                entry("linear_drag_coefficient", 2)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createBubbleColumnUp() {
        return new BedrockParticleDefinition(
                "bubble_column_up",
                "1.26.10",
                "minecraft:bubble_column_up_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 1000)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("Math.random(-1, 1)", 0, "Math.random(-1, 1)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.05 * variable.particle_random_1 + 0.1) * (variable.particle_random_2 * 0.9 + 0.2)", "(0.05 * variable.particle_random_1 + 0.1) * (variable.particle_random_2 * 0.9 + 0.2)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_expire_if_not_in_blocks", List.of("minecraft:water", "minecraft:flowing_water", "minecraft:bubble_column")),
                        entry("minecraft:particle_initial_speed", "Math.random(0, 0.8)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 10)
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 4, 0)),
                                entry("linear_drag_coefficient", 5.25)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createCampfireSmoke() {
        return new BedrockParticleDefinition(
                "campfire_smoke",
                "1.26.10",
                "minecraft:campfire_smoke_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 200)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", "Math.random(0.5, 1.0)"),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.size = 0.75 * math.pow(1.0-(variable.particle_age / variable.particle_lifetime), 0.08);")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 5)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("enabled", "variable.particle_age > 1"),
                                entry("collision_radius", 0.25),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.4, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createCampfireSmokeTall() {
        return new BedrockParticleDefinition(
                "campfire_smoke_tall",
                "1.26.10",
                "minecraft:campfire_tall_smoke_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 200)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", "Math.random(0.5, 1.0)"),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.size = 0.75 * math.pow(1.0-(variable.particle_age / variable.particle_lifetime), 0.08);")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 9)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("enabled", "variable.particle_age > 1"),
                                entry("collision_radius", 0.25),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.4, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createCandleFlame() {
        return new BedrockParticleDefinition(
                "candle_flame",
                "1.26.10",
                "minecraft:candle_flame_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 0.012),
                                entry("direction", List.of(0, 0, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("0.5 * ((0.1 + variable.particle_random_1*0.1) - (0.01 * variable.particle_age))", "0.5 * ((0.1 + variable.particle_random_1*0.1) - (0.01 * variable.particle_age))")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(9.0, 11.5)")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createCherryLeavesParticle() {
        return new BedrockParticleDefinition(
                "cherry_leaves_particle",
                "1.26.10",
                "minecraft:cherry_leaves_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.fall_acc = -0.3;v.wind_acc = 2;v.curve_len = 15;")
                        )),
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 2000)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, -0.075, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("direction", "outwards")
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("v.particle_random_1 > 0.5 ? 0.05 : 0.075", "v.particle_random_1 > 0.5 ? 0.05 : 0.075")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initial_spin", Map.ofEntries(
                                entry("rotation", "math.random(0,360)"),
                                entry("rotation_rate", "math.random(-30,30)")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 15)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("collision_radius", 0.075),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("math.cos(v.particle_random_3 * 60) * v.wind_acc * math.pow(math.min(v.curve_len, v.particle_age) / v.curve_len, 1.25)", "v.fall_acc", "math.sin(v.particle_random_3 * 60) * v.wind_acc * math.pow(math.min(v.curve_len, v.particle_age) / v.curve_len, 1.25)")),
                                entry("rotation_acceleration", "v.particle_random_4 > 0.5 ? -5 : 5")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createCreakingHeartTrail() {
        return new BedrockParticleDefinition(
                "creaking_heart_trail",
                "1.26.10",
                "minecraft:creaking_heart_trail",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.0375, 0.0375)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color.r", "variable.color.g", "variable.color.b", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "variable.particle_initial_speed"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "variable.max_lifetime")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createCrimsonSporeParticle() {
        return new BedrockParticleDefinition(
                "crimson_spore.particle",
                "1.10.0",
                "geyseropt:crimson_spore",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.75)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.75)
                        )),
                        entry("minecraft:particle_initial_speed", 2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 4, 0)),
                                entry("linear_drag_coefficient", 1)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.24, 0.24)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(1, 0.50196, 0.41176, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createCriticalHit() {
        return new BedrockParticleDefinition(
                "critical_hit",
                "1.26.10",
                "minecraft:critical_hit_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.15)
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 520),
                                entry("max_particles", "variable.particle_count ?? 48.0")
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0, "Math.random(-0.9, -0.5)", 0)),
                                entry("direction", List.of("Math.random(-0.75, 0.75)", "Math.random(-1.0, 1.0)", "Math.random(-0.75, 0.75)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("0.10 + variable.particle_random_1*0.05", "0.10 + variable.particle_random_1*0.05")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.3 + 0.6", "variable.particle_random_2 * 0.3 + 0.6", "variable.particle_random_2 * 0.3 + 0.6", 1)),
                                                entry("1.000000", List.of("variable.particle_random_1 * 0.3 + 0.6", 0.5, 0.3, 1))
                                        )),
                                        entry("interpolant", "variable.particle_age/variable.particle_lifetime")
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(10.0, 20.0)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "6.0 / (Math.random(0.0, 16.0) + 12.0)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -12, 0)),
                                entry("linear_drag_coefficient", 10)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createDamageIndicatorParticle() {
        return new BedrockParticleDefinition(
                "damage_indicator.particle",
                "1.10.0",
                "geyseropt:damage_indicator",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1.5)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 1.5)
                        )),
                        entry("minecraft:particle_initial_speed", 0.1),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "math.pow(6.5, -2.5 * variable.particle_age) * (1 - 6*variable.particle_age)*50", 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.2, 0.2)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("interpolant", "variable.particle_age / variable.particle_lifetime"),
                                        entry("gradient", Map.ofEntries(
                                                entry("0.0", "#FFC66464"),
                                                entry("1.0", "#FF4C0000")
                                        ))
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createDolphinMoveParticle() {
        return new BedrockParticleDefinition(
                "dolphin_move_particle",
                "1.26.10",
                "minecraft:dolphin_move_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.max_spawn_rate_speed = 15.0f; variable.max_spawn_rate_speed_scalar = 10.0f;")
                        )),
                        entry("minecraft:emitter_lifetime_expression", Map.ofEntries(
                                entry("activation_expression", "query.is_in_water")
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", "Math.random(1.579, 2.632) * Math.lerp(1.0f, (variable.max_spawn_rate_speed_scalar ?? 10.0f), Math.clamp(Math.inverse_lerp(0.0f, (variable.max_spawn_rate_speed ?? 15.0f), query.ground_speed), 0.0f, 1.0f))"),
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("Math.random(-1, 1) * 0.02", "Math.random(-1, 1) * 0.02", "Math.random(-1, 1) * 0.02"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("((0.05*variable.particle_random_1+0.1)*(variable.particle_random_2*0.9+0.2))", "((0.05*variable.particle_random_1+0.1)*(variable.particle_random_2*0.9+0.2))")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_expire_if_not_in_blocks", List.of("minecraft:water", "minecraft:flowing_water", "minecraft:bubble_column")),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "2 / ((Math.Random(0.0, 1.0) * 0.8 + 0.2) * 5)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.8, 0)),
                                entry("linear_drag_coefficient", 5.25)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createDragonBreathLingering() {
        return new BedrockParticleDefinition(
                "dragon_breath_lingering",
                "1.26.10",
                "minecraft:dragon_breath_lingering",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", "variable.cloud_lifetime")
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", "Math.max(variable.cloud_radius * variable.cloud_radius * variable.particle_multiplier, 2)")
                        )),
                        entry("minecraft:emitter_shape_disc", Map.ofEntries(
                                entry("radius", "variable.cloud_radius"),
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "rotate_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.863, 0, 0.937, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(2.0, 3.25)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.75, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createDripstoneLavaDrip() {
        return new BedrockParticleDefinition(
                "dripstone_lava_drip",
                "1.26.10",
                "minecraft:stalactite_lava_drip_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0, 0.1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, "0.8 / (variable.stuck_time - (variable.stuck_time-variable.particle_age) + 0.8)", "0.2 / (variable.stuck_time - (variable.stuck_time-variable.particle_age) + 0.4)", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = 2.0;"),
                                entry("per_render_expression", "variable.stuck_time = 2.0;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 5)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("events", List.of(Map.ofEntries(
                                        entry("event", "hit_ground")
                                )))
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -19.6, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = 2.0; return variable.particle_age < variable.stuck_time ? 277 : 0.01;")
                        ))
                ),
                Map.ofEntries(
                        entry("hit_ground", Map.ofEntries(
                                entry("sound_effect", Map.ofEntries(
                                        entry("event_name", "drip.lava.pointed_dripstone")
                                ))
                        ))
                ),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createDripstoneWaterDrip() {
        return new BedrockParticleDefinition(
                "dripstone_water_drip",
                "1.26.10",
                "minecraft:stalactite_water_drip_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.2, 0.3, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = 2.0;"),
                                entry("per_render_expression", "variable.stuck_time = 2.0;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 5)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of(Map.ofEntries(
                                        entry("event", "hit_ground")
                                ), Map.ofEntries(
                                        entry("event", "splash")
                                ), Map.ofEntries(
                                        entry("event", "splash")
                                ), Map.ofEntries(
                                        entry("event", "splash")
                                )))
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -19.6, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = 2.0; return variable.particle_age < variable.stuck_time ? 500 : 0.01;")
                        ))
                ),
                Map.ofEntries(
                        entry("hit_ground", Map.ofEntries(
                                entry("sound_effect", Map.ofEntries(
                                        entry("event_name", "drip.water.pointed_dripstone")
                                ))
                        )),
                        entry("splash", Map.ofEntries(
                                entry("particle_effect", Map.ofEntries(
                                        entry("effect", "minecraft:rain_splash_particle"),
                                        entry("type", "particle")
                                ))
                        ))
                ),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createDustPlume() {
        return new BedrockParticleDefinition(
                "dust_plume",
                "1.26.10",
                "minecraft:dust_plume",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 7)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.45, 0.45)", 0.95, "Math.random(-0.45, 0.45)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.165, 0.165)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("0.73 - variable.particle_random_1 * 0.2", "0.69 - variable.particle_random_1 * 0.2", "0.76 - variable.particle_random_1 * 0.2", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(1.5, 3.0)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "7.0 / 20.0 / Math.random(0.5, 1.0)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "-2.0 * Math.pow(0.88, variable.particle_age)", 0)),
                                entry("linear_drag_coefficient", "1.96 * Math.pow(0.92, variable.particle_age)")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createElectricSpark() {
        return new BedrockParticleDefinition(
                "electric_spark",
                "1.26.10",
                "minecraft:electric_spark_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.size = Math.random(0.1, 0.2);")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("(variable.particle_random_1 * variable.direction.x) - (variable.direction.x / 2.0)", "(variable.particle_random_2 * variable.direction.y) - (variable.direction.y / 2.0)", "(variable.particle_random_3 * variable.direction.z) - (variable.direction.z / 2.0)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 0.9, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", -1),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "((variable.particle_random_1 * 2) + 3) / 20")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 0.96)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createEnchantedHitSingleParticle() {
        return new BedrockParticleDefinition(
                "enchanted_hit_single.particle",
                "1.10.0",
                "geyseropt:enchanted_hit_single",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.25)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.25)
                        )),
                        entry("minecraft:particle_initial_speed", 0.2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -5, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.1, 0.1)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(0.26667, 0.60392, 0.58039, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createEnchantingTableParticle() {
        return new BedrockParticleDefinition(
                "enchanting_table_particle",
                "1.26.10",
                "minecraft:enchanting_table_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_random_1 * 0.05 + 0.02", "variable.particle_random_1 * 0.05 + 0.02")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_3 * 0.54 + 0.36", "variable.particle_random_3 * 0.54 + 0.36", "variable.particle_random_3 * 0.6 + 0.4", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.5, 2.5)")
                        )),
                        entry("minecraft:particle_motion_parametric", Map.ofEntries(
                                entry("relative_position", List.of("variable.pos = 1 - (variable.particle_age / variable.particle_lifetime); return variable.direction.x * variable.pos;", "variable.direction.y * variable.pos - Math.pow((1 - variable.pos), 3) * 1.2", "variable.direction.z * variable.pos"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createEndrod() {
        return new BedrockParticleDefinition(
                "endrod",
                "1.26.10",
                "minecraft:endrod",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of(0, -1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(variable.particle_random_1 * 0.5f + 0.5f) * 0.2", "(variable.particle_random_1 * 0.5f + 0.5f) * 0.2")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_age > (variable.particle_lifetime / 2.0) ? 1 - (0.0153 * (1 - Math.pow(0.7, variable.particle_age)) / (1 - 0.7)) : 1.0", "variable.particle_age > (variable.particle_lifetime / 2.0) ? 1 - (0.0387 * (1 - Math.pow(0.7, variable.particle_age)) / (1 - 0.7)) : 1.0", "variable.particle_age > (variable.particle_lifetime / 2.0) ? 1 - (0.0636 * (1 - Math.pow(0.7, variable.particle_age)) / (1 - 0.7)) : 1.0", "variable.particle_age > (variable.particle_lifetime / 2.0) ? 1 - 0.60 * ((variable.particle_age - (variable.particle_lifetime / 2.0)) / (variable.particle_lifetime / 2.0)) : 1.0"))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(3, 3.6)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -0.2, 0)),
                                entry("linear_drag_coefficient", 0.49)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createEvokerSpell() {
        return new BedrockParticleDefinition(
                "evoker_spell",
                "1.26.10",
                "minecraft:evoker_spell",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_looping", Map.ofEntries(
                                entry("active_time", 10)
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 20),
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("-0.5 + Math.Random(0, 1) + (Math.Random(0, 2) - 1) * 0.4", "(1 + (Math.Random(0, 2) - 1) * 0.4) * 0.5", "-0.5 + Math.Random(0, 1) + (Math.Random(0, 2) - 1) * 0.4"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("query.spellcolor.r", "query.spellcolor.g", "query.spellcolor.b", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "(Math.Random(0, 1) + Math.Random(0, 1) + 1) * 0.6"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(0.4, 2.0)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_radius", 0.2),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.1, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createExplosionManual() {
        return new BedrockParticleDefinition(
                "explosion_manual",
                "1.26.10",
                "minecraft:explosion_manual",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("Math.random(-0.5, 0.5)", 0, "Math.random(-0.5, 0.5)")),
                                entry("direction", List.of("Math.random(0, 0.02)", "Math.random(0, 0.02)", "Math.random(0, 0.02)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1", "variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", 0))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 2),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "4 / Math.Random(1, 5) + 0.1")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.1),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 2, 0)),
                                entry("linear_drag_coefficient", 2.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createFallingDust() {
        return new BedrockParticleDefinition(
                "falling_dust",
                "1.26.10",
                "minecraft:falling_dust",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_random_3 * 0.075 + 0.075", "variable.particle_random_3 * 0.075 + 0.075")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color.r", "variable.color.g", "variable.color.b", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of(0, 0, 0)),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "1.44 / math.random(0.2, 1.0)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -1, 0))
                        )),
                        entry("minecraft:particle_motion_parametric", Map.ofEntries(
                                entry("rotation", "variable.particle_random_1 * 360 + (variable.particle_age * 100 * (variable.particle_random_2 - 0.5))")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createFireflyParticle() {
        return new BedrockParticleDefinition(
                "firefly_particle",
                "1.26.10",
                "minecraft:firefly_particle",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.acceleration_x = math.random( -5, 5 ); v.acceleration_y = math.random( -5, 5 ); v.acceleration_z = math.random( -5, 5 );"),
                                entry("per_update_expression", "v.should_change = math.random( 0, 1 ) <= 0.05; v.should_change ? ( v.acceleration_x = math.random( -5, 5 ) ); v.should_change ? ( v.acceleration_y = math.random( -5, 5 ) ); v.should_change ? ( v.acceleration_z = math.random( -5, 5 ) );")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.0375, 0.0375)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 0.92156869, 0)),
                                                entry("0.300000", List.of(1, 1, 0.92156869, 1)),
                                                entry("0.500000", List.of(1, 1, 0.92156869, 1)),
                                                entry("1.000000", List.of(1, 1, 0.92156869, 0))
                                        )),
                                        entry("interpolant", "v.particle_age / v.particle_lifetime")
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0.5),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "math.random( 10, 15 )")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("coefficient_of_restitution", 2),
                                entry("collision_radius", 0.0375),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("v.acceleration_x", "v.acceleration_y", "v.acceleration_z")),
                                entry("linear_drag_coefficient", 3)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createFlashParticle() {
        return new BedrockParticleDefinition(
                "flash.particle",
                "1.10.0",
                "geyseropt:flash",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("per_update_expression", "v.visible = math.random_integer(0, 1);")
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.3)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.3)
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_motion_dynamic", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(6, 6)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("interpolant", "v.visible"),
                                        entry("gradient", Map.ofEntries(
                                                entry("0.0", "#FFFFFFFF"),
                                                entry("1.0", "#00FFFFFF")
                                        ))
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createGeyserBaseParticle() {
        return new BedrockParticleDefinition(
                "geyser_base.particle",
                "1.26.30",
                "minecraft:geyser_base",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 31),
                                entry("max_particles", 45)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", "variable.active_time")
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0, 0.2, 0)),
                                entry("half_dimensions", List.of(0.25, 0.25, 0.25)),
                                entry("surface_only", false),
                                entry("direction", List.of("2 * variable.particle_random_1 - 1", "math.abs(2 * variable.particle_random_2 - 1)", "2 * variable.particle_random_3 - 1"))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "1.0 + 0.25 * math.random(0, 1)")
                        )),
                        entry("minecraft:particle_initial_speed", 10),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0, 0)),
                                entry("linear_drag_coefficient", 4)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("0.125 * (3.0 + 0.025 * variable.plume_height)", "0.125 * (3.0 + 0.025 * variable.plume_height)")),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 5),
                                entry("coefficient_of_restitution", 0),
                                entry("collision_radius", 0.35)
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createGeyserPlumeParticle() {
        return new BedrockParticleDefinition(
                "geyser_plume.particle",
                "1.26.30",
                "minecraft:geyser_plume",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_local_space", Map.ofEntries(
                                entry("position", true),
                                entry("rotation", false),
                                entry("velocity", true)
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", "7 * variable.plume_height"),
                                entry("max_particles", "5 * variable.plume_height")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", "variable.active_time")
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of("Math.random(-0.4, 0.4)", 0.5, "Math.random(-0.4, 0.4)")),
                                entry("half_dimensions", List.of(0.1, 0.5, 0.1)),
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "math.ln(1 + 0.15 * (3.74 * math.sqrt(variable.plume_height) + 1.04 * variable.plume_height)) / 1.2")
                        )),
                        entry("minecraft:particle_initial_speed", "3 * math.sqrt(variable.plume_height) + 1.04 * variable.plume_height"),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(2 * variable.particle_random_1 - 1) * 18 * math.pow(variable.particle_age / variable.particle_lifetime, 2)", "-8 + 6 * math.pow(variable.particle_age / variable.particle_lifetime, 2)", "(2 * variable.particle_random_2 - 1) * 18 * math.pow(variable.particle_age / variable.particle_lifetime, 2)")),
                                entry("linear_drag_coefficient", 1.2)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("0.12 * (2.0 + variable.plume_height / 8.0 + 1.0 * (variable.particle_age / variable.particle_lifetime))", "0.12 * (2.0 + variable.plume_height / 8.0 + 1.0 * (variable.particle_age / variable.particle_lifetime))")),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 5),
                                entry("coefficient_of_restitution", 0),
                                entry("collision_radius", 0.1)
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createGeyserPoofParticle() {
        return new BedrockParticleDefinition(
                "geyser_poof.particle",
                "1.26.30",
                "minecraft:geyser_poof",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 20)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.5)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0, 0.2, 0)),
                                entry("half_dimensions", List.of(0.25, 0.25, 0.25)),
                                entry("direction", List.of("2 * variable.particle_random_1 - 1", "math.abs(2 * variable.particle_random_2 - 1)", "2 * variable.particle_random_3 - 1"))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "1.0 + 0.25 * math.random(0, 1)")
                        )),
                        entry("minecraft:particle_initial_speed", 10),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0, 0)),
                                entry("linear_drag_coefficient", 6)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("0.125 * (3.0 + 0.025 * variable.plume_height)", "0.125 * (3.0 + 0.025 * variable.plume_height)")),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createGlow() {
        return new BedrockParticleDefinition(
                "glow",
                "1.26.10",
                "minecraft:glow_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x * 0.2 + Math.random(-1, 1) * 0.02", "variable.direction.y * 0.2 + Math.random(-1, 1) * 0.02", "variable.direction.z * 0.2 + Math.random(-1, 1) * 0.02"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.05*variable.particle_random_1+0.1)*(variable.particle_random_2*0.9+0.2)", "(0.05*variable.particle_random_1+0.1)*(variable.particle_random_2*0.9+0.2)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color.r", "variable.color.g", "variable.color.b", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "2 / ((Math.Random(0.0, 1.0) * 0.8 + 0.2) * 5)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.8, 0)),
                                entry("linear_drag_coefficient", 5.25)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createGreenFlame() {
        return new BedrockParticleDefinition(
                "green_flame",
                "1.26.10",
                "minecraft:green_flame_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 0.025),
                                entry("direction", List.of(0, 0, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.1 + variable.particle_random_1*0.1) - (0.1 * variable.particle_age)", "(0.1 + variable.particle_random_1*0.1) - (0.1 * variable.particle_age)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.6, 2.0)")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createHeart() {
        return new BedrockParticleDefinition(
                "heart",
                "1.26.10",
                "minecraft:heart_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 0.025),
                                entry("direction", List.of(0, 0.1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.2 + variable.particle_random_1*0.1)", "(0.2 + variable.particle_random_1*0.1)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", 2.5),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.2, 1.5)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createHoneyDrip() {
        return new BedrockParticleDefinition(
                "honey_drip",
                "1.26.10",
                "minecraft:honey_drip_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, 0, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("surface_only", true),
                                entry("direction", List.of(0, 0, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.582, 0.448, 0.082, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = variable.particle_random_1 * 4 + 1;"),
                                entry("per_render_expression", "variable.stuck_time = variable.particle_random_1 * 4 + 1;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 10)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("events", List.of(Map.ofEntries(
                                        entry("event", "hit_ground")
                                )))
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -3.5, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = variable.particle_random_1 * 4 + 1; return variable.particle_age < variable.stuck_time ? 277 : 0.01;")
                        ))
                ),
                Map.ofEntries(
                        entry("hit_ground", Map.ofEntries(
                                entry("sound_effect", Map.ofEntries(
                                        entry("event_name", "block.beehive.drip")
                                ))
                        ))
                ),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createInfestedEmitter() {
        return new BedrockParticleDefinition(
                "infested_emitter",
                "1.26.10",
                "minecraft:infested_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.25, 0.25)", "Math.random(0, 1)", "Math.random(-0.25, 0.25)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", "(Math.random(0, 1) + Math.random(0, 1) + 1) * 0.55"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.85, 1.35)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 1),
                                entry("coefficient_of_restitution", 1),
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(variable.particle_random_1 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5", "1 + (0.08 * variable.particle_age) - (variable.particle_age * 0.4) * 1.5", "(variable.particle_random_2 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createInk() {
        return new BedrockParticleDefinition(
                "ink",
                "1.26.10",
                "minecraft:ink_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.ofEntries(
                                entry("activation_expression", "(variable.emitter_age > 0.45 ? 1.0 : 0.0)"),
                                entry("expiration_expression", "(variable.emitter_age > 10.0 ? 1.0 : 0.0)")
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", "variable.particlecount")
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction_vector.x + Math.random(0, 1) * variable.aabb_dimension.x * 2 - variable.aabb_dimension.x", "variable.direction_vector.y + 0.5 + Math.random(0, 1) * variable.aabb_dimension.y", "variable.direction_vector.z + Math.random(0, 1) * variable.aabb_dimension.x * 2 - variable.aabb_dimension.x"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particlesize", "variable.particlesize")),
                                entry("facing_camera_mode", "rotate_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color.r + Math.random(0, 0.05)", "variable.color.g + Math.random(0, 0.05)", "variable.color.b + Math.random(0, 0.05)", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "(1.0 + Math.random(0, 1) + Math.random(0, 1))"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "(variable.particlesize * 4) / (Math.random(0.2, 1))")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 4),
                                entry("collision_radius", 0.1),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "variable.is_outside_water * (-3)", 0)),
                                entry("linear_drag_coefficient", 1.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createLandingHoneyParticle() {
        return new BedrockParticleDefinition(
                "landing_honey.particle",
                "1.10.0",
                "geyseropt:landing_honey",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 7)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 7)
                        )),
                        entry("minecraft:particle_initial_speed", 0.2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -12, 0)),
                                entry("linear_drag_coefficient", 2)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.16666666666666666, 0.16)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 100),
                                entry("collision_radius", 0.1)
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(0.48627, 0.37647, 0, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createLandingLavaParticle() {
        return new BedrockParticleDefinition(
                "landing_lava.particle",
                "1.10.0",
                "geyseropt:landing_lava",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1.5)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 1.5)
                        )),
                        entry("minecraft:particle_initial_speed", 0.2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -12, 0)),
                                entry("linear_drag_coefficient", 2)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.16666666666666666, 0.16)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 100),
                                entry("collision_radius", 0.1)
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(1, 0.36471, 0.2549, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createLandingObsidianTearParticle() {
        return new BedrockParticleDefinition(
                "landing_obsidian_tear.particle",
                "1.10.0",
                "geyseropt:landing_obsidian_tear",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 2)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 2)
                        )),
                        entry("minecraft:particle_initial_speed", 0.2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -12, 0)),
                                entry("linear_drag_coefficient", 2)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.16666666666666666, 0.16)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 100),
                                entry("collision_radius", 0.1)
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(0.50588, 0, 0.7098, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createLargeExplosionLevel() {
        return new BedrockParticleDefinition(
                "large_explosion_level",
                "1.26.10",
                "minecraft:large_explosion",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(1, 1)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.6 + 0.4", "variable.particle_random_1 * 0.6 + 0.4", "variable.particle_random_1 * 0.6 + 0.4", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.3, 0.5)")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createLavaDrip() {
        return new BedrockParticleDefinition(
                "lava_drip",
                "1.26.10",
                "minecraft:lava_drip_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0, 0.1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, "0.8 / (variable.stuck_time - (variable.stuck_time-variable.particle_age) + 0.8)", "0.2 / (variable.stuck_time - (variable.stuck_time-variable.particle_age) + 0.4)", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = 2.0;"),
                                entry("per_render_expression", "variable.stuck_time = 2.0;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 5)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -19.6, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = 2.0; return variable.particle_age < variable.stuck_time ? 277 : 0.01;")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createLavaParticle() {
        return new BedrockParticleDefinition(
                "lava_particle",
                "1.10.0",
                "minecraft:lava_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.1, 0.1)", "Math.random(0.1, 1.0)", "Math.random(-0.1, 0.1)"))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(5.0, 10.0)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.5, 2.0)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(variable.particle_random_1 * 5.0 - 2.5)", -15.0, "(variable.particle_random_2 * 5.0 - 2.5)"))
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 50.0),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.1)
                        )),
                        entry("minecraft:particle_lifetime_events", Map.ofEntries(
                                entry("timeline", Map.ofEntries(
                                        entry("0.0", "smoke"),
                                        entry("0.025", "smoke"),
                                        entry("0.05", "smoke"),
                                        entry("0.075", "smoke"),
                                        entry("0.1", "smoke"),
                                        entry("0.15", "smoke"),
                                        entry("0.2", "smoke"),
                                        entry("0.25", "smoke"),
                                        entry("0.3", "smoke"),
                                        entry("0.35", "smoke"),
                                        entry("0.4", "smoke"),
                                        entry("0.5", "smoke"),
                                        entry("0.6", "smoke"),
                                        entry("0.7", "smoke"),
                                        entry("0.8", "smoke"),
                                        entry("0.9", "smoke"),
                                        entry("1.0", "smoke"),
                                        entry("1.2", "smoke"),
                                        entry("1.4", "smoke"),
                                        entry("1.6", "smoke"),
                                        entry("1.8", "smoke"),
                                        entry("2.0", "smoke")
                                ))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("((variable.particle_random_3 * 0.15 + 0.025) * (1 - Math.pow((variable.particle_age + query.frame_alpha) / variable.particle_lifetime, 2))) * 2", "((variable.particle_random_3 * 0.15 + 0.025) * (1 - Math.pow((variable.particle_age + query.frame_alpha) / variable.particle_lifetime, 2))) * 2")),
                                entry("facing_camera_mode", "lookat_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.ofEntries(
                        entry("smoke", Map.ofEntries(
                                entry("particle_effect", Map.ofEntries(
                                        entry("effect", "minecraft:basic_smoke_particle"),
                                        entry("type", "particle_with_velocity")
                                ))
                        ))
                ),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createLlamaSpit() {
        return new BedrockParticleDefinition(
                "llama_spit",
                "1.26.10",
                "minecraft:llama_spit_smoke",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.05, 0.05)", "Math.random(-0.05, 0.05)", "Math.random(-0.05, 0.05)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1", "variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", 0))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of("Math.random(-0.25, 0.25)", 0.6, "Math.random(-0.25, 0.25)")),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "4 / Math.Random(1, 5) + 0.1")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.1),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createMobPortal() {
        return new BedrockParticleDefinition(
                "mob_portal",
                "1.26.10",
                "minecraft:mob_portal",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 1000)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of(0, -1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color", "variable.color * 0.3", "variable.color * 0.9", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 1),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.xaccel = variable.particle_random_3 - 0.5; variable.zaccel = variable.particle_random_4 - 0.5;"),
                                entry("per_render_expression", "variable.size = (variable.particle_random_2 * 0.02 + 0.05) * (1 - Math.Pow((1-(variable.particle_age/variable.particle_lifetime)),2)); variable.color = (variable.particle_random_1 * 0.6 + 0.4);")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(2.0,2.45)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("variable.xaccel", 0.55, "variable.zaccel"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createMobspell() {
        return new BedrockParticleDefinition(
                "mobspell",
                "1.26.10",
                "minecraft:mobspell_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.25, 0.25)", "Math.random(0, 1)", "Math.random(-0.25, 0.25)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color.r", "variable.color.g", "variable.color.b", "variable.color.a"))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "(Math.random(0, 1) + Math.random(0, 1) + 1) * 0.55"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.85, 1.35)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 1),
                                entry("coefficient_of_restitution", 1),
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(variable.particle_random_1 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5", "1 + (0.08 * variable.particle_age) - (variable.particle_age * 0.4) * 1.5", "(variable.particle_random_2 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createMyceliumDust() {
        return new BedrockParticleDefinition(
                "mycelium_dust",
                "1.26.10",
                "minecraft:mycelium_dust_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.05 * variable.particle_random_1 + 0.1) * (variable.particle_random_2 * 0.6 + 0.5)", "(0.05 * variable.particle_random_1 + 0.1) * (variable.particle_random_2 * 0.6 + 0.5)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_3 * 0.1 + 0.2", "variable.particle_random_3 * 0.1 + 0.2", "variable.particle_random_3 * 0.1 + 0.2", 0))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of("Math.random(-0.008, 0.008)", "Math.random(-0.008, 0.008)", "Math.random(-0.008, 0.008)")),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(5, 1)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 0.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createNautilusParticle() {
        return new BedrockParticleDefinition(
                "nautilus.particle",
                "1.10.0",
                "geyseropt:nautilus",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.size = math.random(0.03, 0.0625);")
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 2)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 2)
                        )),
                        entry("minecraft:particle_initial_speed", 0.2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -3, 0)),
                                entry("linear_drag_coefficient", 2)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("v.size", "v.size")),
                                entry("facing_camera_mode", "rotate_xyz")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createNectarDrip() {
        return new BedrockParticleDefinition(
                "nectar_drip",
                "1.26.10",
                "minecraft:nectar_drip_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", "Math.random(1, 4)"),
                                entry("max_particles", 20)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of("Math.random(-0.3, 0.3)", 0.1, "Math.random(-0.3, 0.3)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.92, 0.782, 0.72, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = 0.1;"),
                                entry("per_render_expression", "variable.stuck_time = 0.1;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 5)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -3.5, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = 0.1; return variable.particle_age < variable.stuck_time ? 277 : 0.01;")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createNote() {
        return new BedrockParticleDefinition(
                "note",
                "1.26.10",
                "minecraft:note_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 0.025),
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.1 + variable.particle_random_1*0.05)", "(0.1 + variable.particle_random_1*0.05)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.note_color.r", "variable.note_color.g", "variable.note_color.b", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 2.5),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.3)
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 2.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createNoxiousGasParticle() {
        return new BedrockParticleDefinition(
                "noxious_gas_particle",
                "1.26.30",
                "minecraft:noxious_gas_particle",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_local_space", Map.ofEntries(
                                entry("position", true),
                                entry("rotation", true),
                                entry("velocity", true)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("0.0", "1.0", "0.0"))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 2)
                        )),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.size = (0.05 + variable.particle_random_1 * 0.05) * 4.5 * math.clamp((v.particle_age / v.particle_lifetime) * 32, 0, 1);")
                        )),
                        entry("minecraft:particle_initial_speed", "math.random(0.05, 0.4)"),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.4, 0)),
                                entry("linear_drag_coefficient", 0.8)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("interpolant", "v.particle_age / v.particle_lifetime"),
                                        entry("gradient", Map.ofEntries(
                                                entry("0.54", "#ffffffff"),
                                                entry("1.0", "#00FFFFFF")
                                        ))
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createObsidianTear() {
        return new BedrockParticleDefinition(
                "obsidian_tear",
                "1.26.10",
                "minecraft:obsidian_tear_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.51171875, 0.03125, 0.890625, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = variable.particle_random_1 * 4 + 1;"),
                                entry("per_render_expression", "variable.stuck_time = variable.particle_random_1 * 4 + 1;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 10)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("events", List.of(Map.ofEntries(
                                        entry("event", "hit_ground")
                                )))
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -3.5, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = variable.particle_random_1 * 4 + 1; return variable.particle_age < variable.stuck_time ? 277 : 0.01;")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createOminousSpawningParticle() {
        return new BedrockParticleDefinition(
                "ominous_spawning_particle",
                "1.26.10",
                "minecraft:ominous_spawning_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", "variable.lifetime")
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 10),
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 2),
                                entry("surface_only", true),
                                entry("direction", "inwards")
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("1 * variable.shrink", "0.5 * variable.shrink")),
                                entry("facing_camera_mode", "lookat_direction"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0))
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.27058825, 0.68235296, 0.99607849, 1)),
                                                entry("1.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", "v.particle_age / v.particle_lifetime")
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "2 * variable.speed"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 1)
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.of())
                ),
                Map.of(),
                Map.ofEntries(
                        entry("variable.shrink", Map.ofEntries(
                                entry("type", "linear"),
                                entry("nodes", List.of(1, 0)),
                                entry("input", "v.particle_age"),
                                entry("horizontal_range", "v.particle_lifetime")
                        )),
                        entry("variable.speed", Map.ofEntries(
                                entry("type", "bezier"),
                                entry("nodes", List.of(1, 2, 2, 2)),
                                entry("input", "v.particle_age"),
                                entry("horizontal_range", "v.particle_lifetime")
                        ))
                )
        );
    }

    private static BedrockParticleDefinition createOrangePoplarLeavesParticle() {
        return new BedrockParticleDefinition(
                "orange_poplar_leaves_particle",
                "1.26.50",
                "minecraft:orange_poplar_leaves_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", Map.ofEntries(
                                        entry("expression", "v.fall_acc = -0.3; v.wind_acc = 10; v.curve_len = 15;"),
                                        entry("version", 12)
                                ))
                        )),
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 2000)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, -0.075, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("direction", "outwards")
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(Map.ofEntries(
                                        entry("expression", "v.particle_random_1 > 0.5 ? 0.1 : 0.15"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "v.particle_random_1 > 0.5 ? 0.1 : 0.15"),
                                        entry("version", 12)
                                ))),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of(0, -0.021, 0)),
                        entry("minecraft:particle_initial_spin", Map.ofEntries(
                                entry("rotation", Map.ofEntries(
                                        entry("expression", "math.random(0, 360)"),
                                        entry("version", 12)
                                )),
                                entry("rotation_rate", Map.ofEntries(
                                        entry("expression", "math.random(-30, 30)"),
                                        entry("version", 12)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 15)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("collision_radius", 0.075),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(Map.ofEntries(
                                        entry("expression", "math.cos((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "v.fall_acc"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "math.sin((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)"),
                                        entry("version", 12)
                                ))),
                                entry("rotation_acceleration", Map.ofEntries(
                                        entry("expression", "v.particle_random_4 > 0.5 ? -5 : 5"),
                                        entry("version", 12)
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createPaleOakLeavesParticle() {
        return new BedrockParticleDefinition(
                "pale_oak_leaves_particle",
                "1.26.10",
                "minecraft:pale_oak_leaves_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.fall_acc = -0.3; v.wind_acc = 10; v.curve_len = 15;")
                        )),
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 2000)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, -0.075, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("direction", "outwards")
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("v.particle_random_1 > 0.5 ? 0.1 : 0.15", "v.particle_random_1 > 0.5 ? 0.1 : 0.15")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of(0, -0.021, 0)),
                        entry("minecraft:particle_initial_spin", Map.ofEntries(
                                entry("rotation", "math.random(0, 360)"),
                                entry("rotation_rate", "math.random(-30, 30)")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 15)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("collision_radius", 0.075),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("math.cos((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)", "v.fall_acc", "math.sin((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)")),
                                entry("rotation_acceleration", "v.particle_random_4 > 0.5 ? -5 : 5")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createPauseMobGrowth() {
        return new BedrockParticleDefinition(
                "pause_mob_growth",
                "1.10.0",
                "minecraft:pause_mob_growth",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 6)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1.0)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("half_dimensions", List.of(0.6, 0.04, 0.6)),
                                entry("offset", List.of(0.0, 0.2, 0.0)),
                                entry("surface_only", false),
                                entry("direction", List.of(0.0, -1.0, 0.0))
                        )),
                        entry("minecraft:particle_initial_speed", 0.3),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(1.3, 2.0)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0.0, -0.08, 0.0)),
                                entry("linear_drag_coefficient", 0.04)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("Math.random(0.12, 0.16)", "Math.random(0.12, 0.16)")),
                                entry("facing_camera_mode", "lookat_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createPortalReverse() {
        return new BedrockParticleDefinition(
                "portal_reverse",
                "1.26.10",
                "minecraft:portal_reverse_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 10)
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 3),
                                entry("max_particles", 20)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, 0, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("surface_only", true),
                                entry("direction", List.of(0, 0, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(variable.particle_random_2 * 0.02 + 0.05) * (1 - (variable.particle_age/variable.particle_lifetime))", "(variable.particle_random_2 * 0.02 + 0.05) * (1 - (variable.particle_age/variable.particle_lifetime))")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.6 + 0.4", "(variable.particle_random_1 * 0.6 + 0.4) * 0.3", "(variable.particle_random_1 * 0.6 + 0.4) * 0.9", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.alpha = variable.particle_age/variable.particle_lifetime; variable.distance = (1-(-1*(variable.alpha)+Math.Pow((variable.alpha),2)*2));"),
                                entry("per_render_expression", "variable.alpha = variable.particle_age/variable.particle_lifetime; variable.distance = (1-(-1*(variable.alpha)+Math.Pow((variable.alpha),2)*2));")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(3.0, 3.1)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("variable.direction.x * variable.distance", "variable.direction.y * variable.distance + variable.alpha", "variable.direction.z * variable.distance"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createRaidOmenAmbient() {
        return new BedrockParticleDefinition(
                "raid_omen_ambient",
                "1.26.10",
                "minecraft:raid_omen_ambient",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.25, 0.25)", "Math.random(0, 1)", "Math.random(-0.25, 0.25)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", "(Math.random(0, 1) + Math.random(0, 1) + 1) * 0.55"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.85, 1.35)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 1),
                                entry("coefficient_of_restitution", 1),
                                entry("collision_radius", 0.5),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(variable.particle_random_1 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5", "1 + (0.08 * variable.particle_age) - (variable.particle_age * 0.4) * 1.5", "(variable.particle_random_2 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createRainSplash() {
        return new BedrockParticleDefinition(
                "rain_splash",
                "1.26.10",
                "minecraft:rain_splash_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 300)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.175, 0.175)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", -6),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(0.19, 0.21)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "30.0 + variable.particle_random_2*60.0 - variable.particle_age*480.0", 0)),
                                entry("linear_drag_coefficient", 20)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createRedPoplarLeavesParticle() {
        return new BedrockParticleDefinition(
                "red_poplar_leaves_particle",
                "1.26.50",
                "minecraft:red_poplar_leaves_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", Map.ofEntries(
                                        entry("expression", "v.fall_acc = -0.3; v.wind_acc = 10; v.curve_len = 15;"),
                                        entry("version", 12)
                                ))
                        )),
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 2000)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, -0.075, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("direction", "outwards")
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(Map.ofEntries(
                                        entry("expression", "v.particle_random_1 > 0.5 ? 0.1 : 0.15"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "v.particle_random_1 > 0.5 ? 0.1 : 0.15"),
                                        entry("version", 12)
                                ))),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of(0, -0.021, 0)),
                        entry("minecraft:particle_initial_spin", Map.ofEntries(
                                entry("rotation", Map.ofEntries(
                                        entry("expression", "math.random(0, 360)"),
                                        entry("version", 12)
                                )),
                                entry("rotation_rate", Map.ofEntries(
                                        entry("expression", "math.random(-30, 30)"),
                                        entry("version", 12)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 15)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("collision_radius", 0.075),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(Map.ofEntries(
                                        entry("expression", "math.cos((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "v.fall_acc"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "math.sin((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)"),
                                        entry("version", 12)
                                ))),
                                entry("rotation_acceleration", Map.ofEntries(
                                        entry("expression", "v.particle_random_4 > 0.5 ? -5 : 5"),
                                        entry("version", 12)
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createResetMobGrowth() {
        return new BedrockParticleDefinition(
                "reset_mob_growth",
                "1.10.0",
                "minecraft:reset_mob_growth",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 6)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1.0)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("half_dimensions", List.of(0.6, 0.04, 0.6)),
                                entry("offset", List.of(0.0, 0.0, 0.0)),
                                entry("surface_only", false),
                                entry("direction", List.of(0.0, 1.0, 0.0))
                        )),
                        entry("minecraft:particle_initial_speed", 0.3),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(1.3, 2.0)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0.0, 0.08, 0.0)),
                                entry("linear_drag_coefficient", 0.04)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("Math.random(0.12, 0.16)", "Math.random(0.12, 0.16)")),
                                entry("facing_camera_mode", "lookat_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSculkCharge() {
        return new BedrockParticleDefinition(
                "sculk_charge",
                "1.26.10",
                "minecraft:sculk_charge_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.size = Math.random(0.15, 0.3);")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.4, 1.0)")
                        )),
                        entry("minecraft:particle_motion_parametric", Map.ofEntries(
                                entry("rotation", "variable.rotation")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSculkChargePop() {
        return new BedrockParticleDefinition(
                "sculk_charge_pop",
                "1.26.10",
                "minecraft:sculk_charge_pop_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.size = Math.random(0.1, 0.2);")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0.7),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.3, 0.5)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSculkSoul() {
        return new BedrockParticleDefinition(
                "sculk_soul",
                "1.26.10",
                "minecraft:sculk_soul_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.15)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 2)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.1 + variable.particle_random_1*0.1) + (0.2 * variable.particle_age)", "(0.1 + variable.particle_random_1*0.1) + (0.2 * variable.particle_age)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "((8 / (Math.random(0.0, 0.8) + 0.2)) + 4) / 20.0")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.4, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createShriek() {
        return new BedrockParticleDefinition(
                "shriek",
                "1.10.0",
                "minecraft:shriek_particle",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.size = 0.85 * math.clamp(variable.particle_age / variable.particle_lifetime * 0.75, 0.0, 1.0);")
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 4),
                                entry("max_particles", 10)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 4)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0.0, 0, 0.0)),
                                entry("direction", List.of(0, 0.2, 0))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 1.5)
                        )),
                        entry("minecraft:particle_initial_speed", 1.4),
                        entry("minecraft:particle_motion_dynamic", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "direction_y"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "custom"),
                                        entry("custom_direction", List.of(0.0, 0.3, -0.5))
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(1, 1, 1, "Math.clamp(1.0 - math.clamp(variable.particle_age / variable.particle_lifetime, 0, 1), 0, 1)"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSneeze() {
        return new BedrockParticleDefinition(
                "sneeze",
                "1.26.10",
                "minecraft:sneeze",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.375, 0.375)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.2, 0.78, 0.55, 0.35))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "2 / ((Math.random(0.0, 1.0) * 0.8 + 0.2) * 5)")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSnowflake() {
        return new BedrockParticleDefinition(
                "snowflake",
                "1.26.10",
                "minecraft:snowflake_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 9)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("0.5 + math.random(-0.2, 0.2)", 1.2, "0.5 + math.random(-0.2, 0.2)")),
                                entry("direction", List.of("Math.random(-1.0, 1.0)", 0.2, "Math.random(-1.0, 1.0)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.87450987, 0.89803928, 0.92941183, 0))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(2.0, 2.5)"),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.size = 0.25 * math.pow(1.0-(variable.particle_age / variable.particle_lifetime), 0.08);")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.5)
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -9.8, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSonicExplosion() {
        return new BedrockParticleDefinition(
                "sonic_explosion",
                "1.26.10",
                "minecraft:sonic_explosion",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(1.5, 1.5)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", 0))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.8)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.1),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 2, 0)),
                                entry("linear_drag_coefficient", 2.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSoul() {
        return new BedrockParticleDefinition(
                "soul",
                "1.26.10",
                "minecraft:soul_particle",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.15)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.1 + variable.particle_random_1*0.1) + (0.2 * variable.particle_age)", "(0.1 + variable.particle_random_1*0.1) + (0.2 * variable.particle_age)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(0.5, 1.0)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "((8 / (Math.random(0.0, 0.8) + 0.2)) + 4) / 20.0")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 0.4, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSparkler() {
        return new BedrockParticleDefinition(
                "sparkler",
                "1.26.10",
                "minecraft:sparkler_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.1, 0.1)", "Math.random(-0.125, 0.125)", "Math.random(-0.1, 0.1)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.075, 0.075)),
                                entry("facing_camera_mode", "rotate_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color.r", "variable.color.g", "variable.color.b", "variable.color.a"))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "(Math.random(0, 1) + Math.random(0, 1) + 1) * 0.09"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "1.7 + Math.random(0, 0.6)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("enabled", 0),
                                entry("collision_drag", 1),
                                entry("coefficient_of_restitution", 1),
                                entry("collision_radius", 0.5),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "(variable.particle_age * 0.4) * -1.5", 0)),
                                entry("linear_drag_coefficient", 1)
                        )),
                        entry("minecraft:particle_motion_parametric", Map.ofEntries(
                                entry("rotation", "math.floor((variable.particle_age)/2) * 45")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSplashpotionspell() {
        return new BedrockParticleDefinition(
                "splashpotionspell",
                "1.26.10",
                "minecraft:splash_spell_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.splash_range ?? { variable.splash_range = 4.0; }; variable.color ?? { variable.color.r = 1.0; variable.color.g = 1.0; variable.color.b = 1.0; variable.color.a = 1.0; }; ")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of("variable.is_vertical = Math.Random(0.0, 1.0) > 0.7; variable.splash_power = Math.random(0.0, 1.0) * variable.splash_range; variable.color_brightness = 0.75 + Math.random(0.0, 1.0) * 0.25; return 0.0;", 0, 0)),
                                entry("direction", List.of("(variable.is_vertical > 0 ? 0.15 : 1.0) * Math.random(-1,1)", "(variable.is_vertical > 0 ? 1.0 : 0.0) * Math.random(0,1)", "(variable.is_vertical > 0 ? 0.15 : 1.0) * Math.random(-1,1)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.color.r * variable.color_brightness", "variable.color.g * variable.color_brightness", "variable.color.b * variable.color_brightness", "variable.color.a"))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "(variable.is_vertical > 0 ? Math.random(1.75, 3.75) : Math.random(1.5, 5.5))"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.7, 1.5)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 1),
                                entry("coefficient_of_restitution", 1),
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(variable.particle_random_1 * 2 - 1) * variable.splash_power * (variable.is_vertical > 0 ? 0.2 : 0.5) * 2.5", "(-0.1 * variable.splash_power + 1) * (variable.is_vertical > 0 ? 0.9 : 1.3) * 2.5", "(variable.particle_random_2 * 2 - 1) * variable.splash_power * (variable.is_vertical > 0 ? 0.2 : 0.5) * 2.5")),
                                entry("linear_drag_coefficient", 2.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSporeBlossomAmbientBlockActor() {
        return new BedrockParticleDefinition(
                "spore_blossom_ambient_block_actor",
                "1.26.10",
                "minecraft:spore_blossom_ambient_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 500)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("Math.random(-10.0, 10.0)", "Math.random(-10.0, 0.0)", "Math.random(-10.0, 10.0)")),
                                entry("direction", List.of("Math.random(-1.0, 1.0)", "Math.random(-0.4, 0.2)", "Math.random(-1.0, 1.0)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.32, 0.5, 0.22, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(2.0, 3.0)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(20, 40)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -0.005, 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSporeBlossomShower() {
        return new BedrockParticleDefinition(
                "spore_blossom_shower",
                "1.26.10",
                "minecraft:spore_blossom_shower_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 200)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, 0.7, 0.5)),
                                entry("half_dimensions", List.of(0.25, 0, 0.25)),
                                entry("surface_only", true),
                                entry("direction", List.of(0, 0, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("return variable.particle_age < variable.stuck_time ? 0.0 : 0.15;", 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.32, 0.5, 0.22, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = variable.particle_random_1 * 7 + 1;"),
                                entry("per_render_expression", "variable.stuck_time = variable.particle_random_1 * 7 + 1;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 10)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -3.5, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = variable.particle_random_1 * 7 + 1; return variable.particle_age < variable.stuck_time ? 200 : 1;")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSulfurBubbleParticle() {
        return new BedrockParticleDefinition(
                "sulfur_bubble_particle",
                "1.26.30",
                "minecraft:sulfur_bubble_particle",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.particle_blend_duration = 16.5f;")
                        )),
                        entry("minecraft:emitter_local_space", Map.ofEntries(
                                entry("position", true),
                                entry("rotation", true),
                                entry("velocity", true)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 2)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x * 0.2 + Math.random(-1, 1) * 0.02", 0, "variable.direction.z * 0.2 + Math.random(-1, 1) * 0.02"))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "16")
                        )),
                        entry("minecraft:particle_expire_if_not_in_blocks", List.of("minecraft:water", "minecraft:flowing_water", "minecraft:bubble_column")),
                        entry("minecraft:particle_kill_plane", List.of(0, 1, 0, 4)),
                        entry("minecraft:particle_initial_speed", "Math.random(0, 0.8)"),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.size_start = 0.02; variable.size_end = 0.15;")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("Math.random(-1, 1) * 2.4", 0.8, "Math.random(-1, 1) * 2.4")),
                                entry("linear_drag_coefficient", 3.25)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("Math.lerp(variable.size_start + variable.size_start * variable.particle_random_1, variable.size_end, Math.min(1.0f, variable.particle_age / variable.particle_blend_duration))", "Math.lerp(variable.size_start + variable.size_start * variable.particle_random_1 , variable.size_end, Math.min(1.0f, variable.particle_age / variable.particle_blend_duration))")),
                                entry("facing_camera_mode", "lookat_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.1),
                                entry("expire_on_contact", true)
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("interpolant", "Math.min(variable.particle_age / 2.0, 1.0)"),
                                        entry("gradient", Map.ofEntries(
                                                entry("0.0", "#171E3942"),
                                                entry("0.5", "#FF567C84"),
                                                entry("1.0", "#ffffffff")
                                        ))
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSulfurCubeParticle() {
        return new BedrockParticleDefinition(
                "sulfur_cube_particle",
                "1.26.10",
                "minecraft:sulfur_cube_goo",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of("Math.random(-0.25f, 0.25f)", "Math.random(-0.25f, 0.25f)", "Math.random(-0.25f, 0.25f)")),
                                entry("direction", List.of("Math.Random(-1.0, 1.0)", "Math.Random(0.0, 1.0)", "Math.Random(-1.0, 1.0)"))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(1, 4)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "0.2f / (Math.random(0.0f, 1.0f) * 0.9f + 0.1f)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -9.8, 0)),
                                entry("linear_drag_coefficient", 0.5)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_drag", 5),
                                entry("collision_radius", 0.1)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(variable.particle_random_1 * 0.04 + 0.04)", "(variable.particle_random_1 * 0.04 + 0.04)")),
                                entry("facing_camera_mode", "lookat_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of())
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createSweepAttackParticle() {
        return new BedrockParticleDefinition(
                "sweep_attack.particle",
                "1.10.0",
                "geyseropt:sweep_attack",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.random = math.random(0,1);")
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.2)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.2)
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_motion_dynamic", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(1, 1)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("interpolant", "v.random"),
                                        entry("gradient", Map.ofEntries(
                                                entry("0.0", "#ffffffff"),
                                                entry("1.0", "#FF7A7A7A")
                                        ))
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createTotem() {
        return new BedrockParticleDefinition(
                "totem",
                "1.26.10",
                "minecraft:totem_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.15)
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 320),
                                entry("max_particles", 48)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0, "Math.random(-0.9, -0.5)", 0)),
                                entry("direction", List.of("Math.random(-1.0, 1.0)", "Math.random(-1.0, 1.0)", "Math.random(-1.0, 1.0)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(0.05 + variable.particle_random_1*0.05)", "(0.05 + variable.particle_random_1*0.05)")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.1, 0.4, 0, 1)),
                                                entry("0.250000", List.of(0.6, 0.6, 0, 1)),
                                                entry("0.500000", List.of(0.3, 0.6, 0.2, 1)),
                                                entry("0.750000", List.of(0.8, 0.8, 0.2, 1)),
                                                entry("1.000000", List.of(0, 0.8, 0, 1))
                                        )),
                                        entry("interpolant", "variable.particle_random_2")
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(10.0, 35.0)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "3.0 + Math.random(0.0, 0.6)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 10)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createTrialOmenEmitter() {
        return new BedrockParticleDefinition(
                "trial_omen_emitter",
                "1.26.10",
                "minecraft:trial_omen_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("direction", List.of("Math.random(-0.25, 0.25)", "Math.random(0, 1)", "Math.random(-0.25, 0.25)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", "(Math.random(0, 1) + Math.random(0, 1) + 1) * 0.55"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.85, 1.35)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 1),
                                entry("coefficient_of_restitution", 1),
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(variable.particle_random_1 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5", "1 + (0.08 * variable.particle_age) - (variable.particle_age * 0.4) * 1.5", "(variable.particle_random_2 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createTrialSpawnerDetectionParticle() {
        return new BedrockParticleDefinition(
                "trial_spawner_detection.particle",
                "1.26.10",
                "minecraft:trial_spawner_detection",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.player_count = 1;")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", "1")
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0, 0, 0)),
                                entry("half_dimensions", List.of(0, 0, 0)),
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(return variable.quadSizeX * math.clamp(variable.particle_age / variable.particle_lifetime * 32, 0, 1);) * 8", "(return variable.quadSizeY * math.clamp(variable.particle_age / variable.particle_lifetime * 32, 0, 1);) * 1.3333333333333333")),
                                entry("facing_camera_mode", "lookat_y"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "math.random(1,4)"),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.quadSizeY = (variable.particle_random_1 * 0.1125) + 0.11; variable.quadSizeX = variable.quadSizeY / 6.0;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "variable.lifetime = math.ceil(8.0 / math.random(0.75, 1.5)); return math.max(variable.lifetime, 1.0)/10.0;")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "math.random(1.0,1.5)", 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createTrialSpawnerDetectionOminousParticle() {
        return new BedrockParticleDefinition(
                "trial_spawner_detection_ominous.particle",
                "1.26.10",
                "minecraft:trial_spawner_detection_ominous",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.player_count = 1;")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", "1")
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0, 0, 0)),
                                entry("half_dimensions", List.of(0, 0, 0)),
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(return variable.quadSizeX * math.clamp(variable.particle_age / variable.particle_lifetime * 32, 0, 1);) * 8", "(return variable.quadSizeY * math.clamp(variable.particle_age / variable.particle_lifetime * 32, 0, 1);) * 1.3333333333333333")),
                                entry("facing_camera_mode", "lookat_y"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "math.random(1,4)"),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_render_expression", "variable.quadSizeY = (variable.particle_random_1 * 0.1125) + 0.11; variable.quadSizeX = variable.quadSizeY / 6.0;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "variable.lifetime = math.ceil(8.0 / math.random(0.75, 1.5)); return math.max(variable.lifetime, 1.0)/10.0;")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "math.random(1.0,1.5)", 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createUnderwaterParticle() {
        return new BedrockParticleDefinition(
                "underwater.particle",
                "1.10.0",
                "geyseropt:underwater",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "v.size = math.random(0.01, 0.02);")
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 2.5)
                        )),
                        entry("minecraft:emitter_shape_sphere", Map.ofEntries(
                                entry("radius", 0.5),
                                entry("direction", "outwards")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 2.5)
                        )),
                        entry("minecraft:particle_initial_speed", 0.3),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 2)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("(v.size) * 8", "(v.size) * 8")),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(0.27843, 0.47843, 0.86667, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createVaultConnection() {
        return new BedrockParticleDefinition(
                "vault_connection",
                "1.26.10",
                "minecraft:vault_connection_particle",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_random_1 * 0.075 + 0.03", "variable.particle_random_1 * 0.075 + 0.03")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_3 * 0.54 + 0.36", "variable.particle_random_3 * 0.54 + 0.36", "variable.particle_random_3 * 0.6 + 0.4", "math.lerp((math.clamp(variable.particle_age / variable.particle_lifetime, 0.2, 0.8) - 0.2) / 0.6, 0.0, 0.6)"))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.5, 2.5)")
                        )),
                        entry("minecraft:particle_motion_parametric", Map.ofEntries(
                                entry("relative_position", List.of("variable.pos = 1 - (variable.particle_age / variable.particle_lifetime); return variable.direction.x * variable.pos;", "variable.direction.y * variable.pos - Math.pow((1 - variable.pos), 3) * 1.2", "variable.direction.z * variable.pos"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createVibrationSignal() {
        return new BedrockParticleDefinition(
                "vibration_signal",
                "1.26.10",
                "minecraft:vibration_signal",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.direction ?? { variable.direction.x = 0.0; variable.direction.y = 1.0; variable.direction.z = 0.0; }; variable.particle_initial_speed ?? { variable.particle_initial_speed = 1.0; }; variable.max_lifetime ?? { variable.max_lifetime = 1.0; }; ")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.15)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.25, 0.25)),
                                entry("facing_camera_mode", "lookat_direction"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "variable.particle_initial_speed"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "variable.max_lifetime")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.of()),
                        entry("minecraft:particle_motion_parametric", Map.ofEntries(
                                entry("rotation", -90)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createVillagerAngry() {
        return new BedrockParticleDefinition(
                "villager_angry",
                "1.26.10",
                "minecraft:villager_angry",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 25)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of(0, 0.1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.14, 0.14)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", 2.5),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.2, 1.5)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createVillagerHappy() {
        return new BedrockParticleDefinition(
                "villager_happy",
                "1.26.10",
                "minecraft:villager_happy",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 25)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("Math.random(0, 0.02)", "Math.random(0, 0.02)", "Math.random(0, 0.02)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.14, 0.14)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(1, 5)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("Math.random(-0.02, 0.02)", "Math.random(-0.02, 0.02)", "Math.random(-0.02, 0.02)"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWarpedSporeParticle() {
        return new BedrockParticleDefinition(
                "warped_spore.particle",
                "1.10.0",
                "geyseropt:warped_spore",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.75)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.75)
                        )),
                        entry("minecraft:particle_initial_speed", 2),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 4, 0)),
                                entry("linear_drag_coefficient", 1)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.24, 0.24)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(0.09804, 0.09804, 0.29804, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWaterDrip() {
        return new BedrockParticleDefinition(
                "water_drip",
                "1.26.10",
                "minecraft:water_drip_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0, 0.1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.15, 0.15)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.2, 0.3, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_initialization", Map.ofEntries(
                                entry("per_update_expression", "variable.stuck_time = 2.0;"),
                                entry("per_render_expression", "variable.stuck_time = 2.0;")
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 5)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of(Map.ofEntries(
                                        entry("event", "splash")
                                ), Map.ofEntries(
                                        entry("event", "splash")
                                ), Map.ofEntries(
                                        entry("event", "splash")
                                )))
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -19.6, 0)),
                                entry("linear_drag_coefficient", "variable.stuck_time = 2.0; return variable.particle_age < variable.stuck_time ? 277 : 0.01;")
                        ))
                ),
                Map.ofEntries(
                        entry("splash", Map.ofEntries(
                                entry("particle_effect", Map.ofEntries(
                                        entry("effect", "minecraft:rain_splash_particle"),
                                        entry("type", "particle")
                                ))
                        ))
                ),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWaterEvaporationActor() {
        return new BedrockParticleDefinition(
                "water_evaporation_actor",
                "1.26.10",
                "minecraft:water_evaporation_actor_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.aabb ?? { variable.aabb.x = 1.0; variable.aabb.y = 1.0; variable.aabb.z = 1.0; };")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 5)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("variable.aabb.x * Math.random(0, 1)", "variable.aabb.y * Math.random(0, 1)", "variable.aabb.z * Math.random(0, 1)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1", "variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", 0))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of("Math.random(-1, 1)", "Math.random(-1, 1)", "Math.random(-1, 1)")),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "4 / Math.Random(1, 5) + 0.1")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.1),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 2, 0)),
                                entry("linear_drag_coefficient", 2.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWaterEvaporationBucket() {
        return new BedrockParticleDefinition(
                "water_evaporation_bucket",
                "1.26.10",
                "minecraft:water_evaporation_bucket_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 8)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("Math.random(0, 1)", "Math.random(0, 1)", "Math.random(0, 1)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1", "variable.particle_random_1 * variable.particle_random_2 * 0.6 + 0.1")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", "variable.particle_random_1 * 0.3 + 0.7", 0))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of("Math.random(-1, 1)", "Math.random(-1, 1)", "Math.random(-1, 1)")),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "4 / Math.Random(1, 5) + 0.1")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.1),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, 2, 0)),
                                entry("linear_drag_coefficient", 2.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWaterSplash() {
        return new BedrockParticleDefinition(
                "water_splash",
                "1.26.10",
                "minecraft:water_splash_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0, 0.5, 0)),
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.2, 0.2)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_expire_if_not_in_blocks", List.of("minecraft:air")),
                        entry("minecraft:particle_initial_speed", -6),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(0.4, 2.0)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(6, "30.0 + variable.particle_random_2*60.0 - variable.particle_age*410.0", 6)),
                                entry("linear_drag_coefficient", 20)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWaterSplashManual() {
        return new BedrockParticleDefinition(
                "water_splash_manual",
                "1.26.10",
                "minecraft:water_splash_particle_manual",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 100)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("offset", List.of(0, 0.5, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.2, 0.2)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_expire_if_not_in_blocks", List.of("minecraft:air")),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(0.4, 2.0)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("coefficient_of_restitution", 0.1),
                                entry("collision_radius", 0.01),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(6, "30.0 + variable.particle_random_2*60.0 - variable.particle_age*410.0", 6)),
                                entry("linear_drag_coefficient", 20)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWaterWake() {
        return new BedrockParticleDefinition(
                "water_wake",
                "1.26.10",
                "minecraft:water_wake_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.particle_life = 3 - variable.particle_lifetime; return variable.particle_life * 0.1;", "variable.particle_life * 0.1")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 3),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.Random(0.4, 2.0)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(2, "variable.particle_random_1 * 4 + 2", 2)),
                                entry("linear_drag_coefficient", 19.6)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWax() {
        return new BedrockParticleDefinition(
                "wax",
                "1.26.10",
                "minecraft:wax_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", "variable.size = Math.random(0.1, 0.2);")
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.ofEntries(
                                entry("direction", List.of("variable.direction.x", "variable.direction.y", "variable.direction.z"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of("variable.size", "variable.size")),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("math.clamp(variable.color.r * 1.2 * math.clamp((variable.particle_age + 0.5) / variable.particle_lifetime, 0, 1), 0, 1)", "math.clamp(variable.color.g * 1.2 * math.clamp((variable.particle_age + 0.5) / variable.particle_lifetime, 0, 1), 0, 1)", "math.clamp(variable.color.b * 1.2 * math.clamp((variable.particle_age + 0.5) / variable.particle_lifetime, 0, 1), 0, 1)", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", -0.05),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "((variable.particle_random_1 * 30) + 10) / 20")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_drag_coefficient", 0.96)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWhiteAshParticle() {
        return new BedrockParticleDefinition(
                "white_ash.particle",
                "1.10.0",
                "geyseropt:white_ash",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 1)
                        )),
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 1)
                        )),
                        entry("minecraft:particle_initial_speed", 1),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, -1, 0)),
                                entry("linear_drag_coefficient", 1)
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.16, 0.16)),
                                entry("facing_camera_mode", "rotate_xyz")
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", List.of(0.81176, 0.81176, 0.81176, 1))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWhiteSmoke() {
        return new BedrockParticleDefinition(
                "white_smoke",
                "1.26.10",
                "minecraft:white_smoke_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 50)
                        )),
                        entry("minecraft:emitter_shape_disc", Map.ofEntries(
                                entry("offset", List.of(0, -0.2, 0)),
                                entry("radius", 0.1),
                                entry("surface_only", true),
                                entry("direction", List.of(0, 1, 0))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(0.72941178, 0.69411767, 0.76078433, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "Math.random(1.0, 1.5)"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.75, 1.25)")
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(0, "Math.random(0.5, 0.8)", 0))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWindChargedAmbient() {
        return new BedrockParticleDefinition(
                "wind_charged_ambient",
                "1.26.10",
                "minecraft:wind_charged_ambient",
                "particles_blend",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 2)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWindChargedEmitter() {
        return new BedrockParticleDefinition(
                "wind_charged_emitter",
                "1.26.10",
                "minecraft:wind_charged_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 1)
                        )),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", 2)
                        )),
                        entry("minecraft:emitter_shape_point", Map.of()),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 0.5)
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWindExplosion() {
        return new BedrockParticleDefinition(
                "wind_explosion",
                "1.26.10",
                "minecraft:wind_explosion_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.ofEntries(
                                entry("active_time", 0.4)
                        )),
                        entry("minecraft:emitter_rate_steady", Map.ofEntries(
                                entry("spawn_rate", 30),
                                entry("max_particles", 10)
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("Math.random(-1, 1)", "Math.random(-1, 1)", "Math.random(-1, 1)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(1, 1)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", 0),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random( 0.6, 0.8 )")
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createWitchspell() {
        return new BedrockParticleDefinition(
                "witchspell",
                "1.26.10",
                "minecraft:witchspell_emitter",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_lifetime_once", Map.of()),
                        entry("minecraft:emitter_rate_instant", Map.ofEntries(
                                entry("num_particles", "Math.random(0, 35) + 10")
                        )),
                        entry("minecraft:emitter_shape_custom", Map.ofEntries(
                                entry("offset", List.of("Math.random(0, 0.13);", "0.5 + Math.random(0, 0.13)", "Math.random(0, 0.13)")),
                                entry("direction", List.of("Math.random(-0.25, 0.25)", "Math.random(0, 1)", "Math.random(-0.25, 0.25)"))
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(0.125, 0.125)),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of("variable.particle_random_1 * 0.5 + 0.35", 0, "variable.particle_random_1 * 0.5 + 0.35", 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", "(Math.random(0, 1) + Math.random(0, 1) + 1) * 0.55"),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", "Math.random(0.85, 1.35)")
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 1),
                                entry("coefficient_of_restitution", 1),
                                entry("collision_radius", 0.01),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of("(variable.particle_random_1 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5", "1 + (0.08 * variable.particle_age) - (variable.particle_age * 0.4) * 1.5", "(variable.particle_random_2 * 2 - 1)*0.5 - (variable.particle_age * 0.4) * 2.5"))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }

    private static BedrockParticleDefinition createYellowPoplarLeavesParticle() {
        return new BedrockParticleDefinition(
                "yellow_poplar_leaves_particle",
                "1.26.50",
                "minecraft:yellow_poplar_leaves_particle",
                "particles_alpha",
                Map.ofEntries(
                        entry("minecraft:emitter_initialization", Map.ofEntries(
                                entry("creation_expression", Map.ofEntries(
                                        entry("expression", "v.fall_acc = -0.3; v.wind_acc = 10; v.curve_len = 15;"),
                                        entry("version", 12)
                                ))
                        )),
                        entry("minecraft:emitter_lifetime_expression", Map.of()),
                        entry("minecraft:emitter_rate_manual", Map.ofEntries(
                                entry("max_particles", 2000)
                        )),
                        entry("minecraft:emitter_shape_box", Map.ofEntries(
                                entry("offset", List.of(0.5, -0.075, 0.5)),
                                entry("half_dimensions", List.of(0.5, 0, 0.5)),
                                entry("direction", "outwards")
                        )),
                        entry("minecraft:particle_appearance_billboard", Map.ofEntries(
                                entry("size", List.of(Map.ofEntries(
                                        entry("expression", "v.particle_random_1 > 0.5 ? 0.1 : 0.15"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "v.particle_random_1 > 0.5 ? 0.1 : 0.15"),
                                        entry("version", 12)
                                ))),
                                entry("facing_camera_mode", "lookat_xyz"),
                                entry("direction", Map.ofEntries(
                                        entry("mode", "derive_from_velocity"),
                                        entry("custom_direction", List.of(0, 0, 0)),
                                        entry("min_speed_threshold", 0.1)
                                ))
                        )),
                        entry("minecraft:particle_appearance_lighting", Map.of()),
                        entry("minecraft:particle_appearance_tinting", Map.ofEntries(
                                entry("color", Map.ofEntries(
                                        entry("gradient", Map.ofEntries(
                                                entry("0.000000", List.of(1, 1, 1, 1))
                                        )),
                                        entry("interpolant", 0)
                                ))
                        )),
                        entry("minecraft:particle_initial_speed", List.of(0, -0.021, 0)),
                        entry("minecraft:particle_initial_spin", Map.ofEntries(
                                entry("rotation", Map.ofEntries(
                                        entry("expression", "math.random(0, 360)"),
                                        entry("version", 12)
                                )),
                                entry("rotation_rate", Map.ofEntries(
                                        entry("expression", "math.random(-30, 30)"),
                                        entry("version", 12)
                                ))
                        )),
                        entry("minecraft:particle_lifetime_expression", Map.ofEntries(
                                entry("max_lifetime", 15)
                        )),
                        entry("minecraft:particle_motion_collision", Map.ofEntries(
                                entry("collision_drag", 10),
                                entry("collision_radius", 0.075),
                                entry("expire_on_contact", true),
                                entry("events", List.of())
                        )),
                        entry("minecraft:particle_motion_dynamic", Map.ofEntries(
                                entry("linear_acceleration", List.of(Map.ofEntries(
                                        entry("expression", "math.cos((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "v.fall_acc"),
                                        entry("version", 12)
                                ), Map.ofEntries(
                                        entry("expression", "math.sin((v.particle_age / 15) * (1000 + v.particle_random_3 * 3000)) * v.wind_acc * (v.particle_age / 15)"),
                                        entry("version", 12)
                                ))),
                                entry("rotation_acceleration", Map.ofEntries(
                                        entry("expression", "v.particle_random_4 > 0.5 ? -5 : 5"),
                                        entry("version", 12)
                                ))
                        ))
                ),
                Map.of(),
                Map.of()
        );
    }
}
