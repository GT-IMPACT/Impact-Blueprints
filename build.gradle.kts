plugins {
    id("minecraft-setup")
    id("publish-setup")
}

impactMod {
    modId.set("iblueprints")
    modName.set("Impact Blueprints")
    modGroup.set("space.impact.iblueprints")
    mavenGroup.set("space.impact")
    mavenUrl.set("https://maven.accident.space/repository/maven-releases/")
    usesMixins.set(true)
}

val modId = impactMod.modId.get()
val modName = impactMod.modName.get()
val modGroup = impactMod.modGroup.get()

buildConfig {
    packageName("space.impact.$modId")
    buildConfigField("String", "MODID", "\"${modId}\"")
    buildConfigField("String", "MODNAME", "\"${modName}\"")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "GROUPNAME", "\"${modGroup}\"")
    buildConfigField("boolean", "IS_DEBUG", "${System.getenv("IMPACT_DEBUG") != null}")
    useKotlinOutput {
        topLevelConstants = true
        internalVisibility = false
    }
}

// RFG's native IDE launcher inherits runtimeClasspath, just like runClient.
val integrationMods = listOf(
    "com.github.GTNewHorizons:NotEnoughItems:2.8.158-GTNH:dev",
    "space.impact:ImpactStructures:1.1.3:dev",
    "space.impact:gregtech-impact:5.09.37.1:dev",
    "com.github.GTNewHorizons:ModularUI2:2.3.92-1.7.10:dev",
)
val integrationRuntime = providers.gradleProperty("sceneIntegrations").map(String::toBoolean).orElse(true)
val angelicaRuntime = providers.gradleProperty("angelica").map(String::toBoolean).orElse(true)

dependencies {
    // GuiContainerWrapper exposes these optional ModularUI interfaces in its superclass hierarchy.
    val viewerInterfaces = listOf(
        "com.github.GTNewHorizons:lwjgl3ify:3.0.34:api",
        "curse.maven:neverenoughanimation-1062347:7664003-sources-7663978",
    )
    viewerInterfaces.forEach {
        compileOnly(it) { isTransitive = false }
        testImplementation(it) { isTransitive = false }
    }
    api(libs.impact.forgelin)
    integrationMods.forEach {
        compileOnly(it) { isTransitive = false }
        if (integrationRuntime.get()) {
            runtimeOnlyNonPublishable(it) { isTransitive = false }
        }
    }
    compileOnly("com.github.GTNewHorizons:CodeChickenCore:1.4.22:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:AE2FluidCraft-Rework:1.5.114-gtnh:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:modularui:1.1.24:dev") { isTransitive = false }
    compileOnly("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-1082-GTNH:dev") { isTransitive = false }
    compileOnly("com.github.GTNewHorizons:GTNHLib:0.11.52:dev") { isTransitive = false }
    if (integrationRuntime.get()) {
        // Explicit support mods: never resolve the full dependency graph of a modpack.
        runtimeOnlyNonPublishable("com.github.GTNewHorizons:CodeChickenCore:1.4.22:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("com.github.GTNewHorizons:modularui:1.1.24:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("com.github.GTNewHorizons:Baubles-Expanded:2.2.24-GTNH:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-1082-GTNH:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("space.impact:GregTechBridge:1.0.2:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("space.impact:VirtualWorld:2.0.4:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("space.impact:Packet-network:1.1.8.dirty:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("space.impact:VisualProspecting:1.3.2:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("org.antlr:antlr4-runtime:4.13.2")
    }
    if (angelicaRuntime.get()) {
        runtimeOnlyNonPublishable("com.github.GTNewHorizons:Angelica:2.2.43:dev") { isTransitive = false }
    }
    if (integrationRuntime.get() || angelicaRuntime.get()) {
        runtimeOnlyNonPublishable("com.github.GTNewHorizons:GTNHLib:0.11.52:dev") { isTransitive = false }
        runtimeOnlyNonPublishable("com.github.GTNewHorizons:GTNHExtLib:1.0.5") { isTransitive = false }
        runtimeOnlyNonPublishable("io.github.legacymoddingmc:unimixins:0.3.1:dev") { isTransitive = false }
    }
    testImplementation("space.impact:ImpactStructures:1.1.3:dev") { isTransitive = false }
    testImplementation("space.impact:gregtech-impact:5.09.37.1:dev") { isTransitive = false }
    testImplementation("com.github.GTNewHorizons:ModularUI2:2.3.92-1.7.10:dev") { isTransitive = false }
    testImplementation("com.github.GTNewHorizons:modularui:1.1.24:dev") { isTransitive = false }
    testImplementation("com.github.GTNewHorizons:Applied-Energistics-2-Unofficial:rv3-beta-1082-GTNH:dev") { isTransitive = false }
    testImplementation("com.github.GTNewHorizons:GTNHLib:0.11.52:dev") { isTransitive = false }
    testImplementation("net.industrial-craft:industrialcraft-2:2.2.828-experimental:dev") { isTransitive = false }
    testImplementation(libs.test.junit)
    testImplementation("com.github.GTNewHorizons:NotEnoughItems:2.8.158-GTNH:dev") { isTransitive = false }
    testImplementation("com.github.GTNewHorizons:CodeChickenCore:1.4.22:dev") { isTransitive = false }
    testRuntimeOnly("it.unimi.dsi:fastutil:8.5.18")
    testRuntimeOnly("org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209")
}

tasks.test {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}
