plugins {
    id("minecraft-setup")
    id("publish-setup")
}

impactMod {
    modId.set("gui3dengine")
    modName.set("GUI 3D Engine")
    modGroup.set("space.impact.gui3dengine")
    mavenGroup.set("space.impact")
    mavenUrl.set("https://maven.accident.space/repository/maven-releases/")
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

dependencies {
    api(libs.impact.forgelin)
}
