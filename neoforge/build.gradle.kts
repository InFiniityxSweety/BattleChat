import org.gradle.kotlin.dsl.support.uppercaseFirstChar

plugins {
    id("com.gradleup.shadow")
    id("multiloader-loader")
    id("net.neoforged.moddev")
    id("dev.isxander.mtk.accessx") version "0.1.1"
    id("me.modmuss50.mod-publish-plugin")
}

repositories {
    // KFF
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
    maven {
        setUrl("https://maven.neoforged.net/releases/")
    }
}
neoForge {
    version = rootProject.property("neoforge_version").toString()
    val at = file("src/main/resources/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
    runs {
        configureEach {
            systemProperty("neoforge.enabledGameTestNamespaces", rootProject.property("mod_id").toString())
            ideName = "NeoForge ${name.replaceFirstChar { c -> c.uppercase() }} (${project.path})" // Unify the run config names with fabric
        }
        create("client") {
            client()
            gameDirectory.set(mkdir(file("runs/client")))
        }
        create("data") {
            clientData()
            gameDirectory.set(mkdir(file("runs/data")))
            // DataGen can be run by - "./gradlew :neoforge:runData" in Terminal.
            // Specify the modid for data generation, where to output the resulting resource, and where to look for existing resources.
            programArguments.addAll(
                listOf(
                    "--mod",
                    rootProject.property("mod_id").toString(),
                    "--all",
                    "--output",
                    file("src/generated/resources/").absolutePath,
                    "--existing",
                    file("src/main/resources/").absolutePath
                )
            )
        }
        create("server") {
            server()
            file("runs/server").parentFile.mkdirs()
            gameDirectory.set(mkdir(file("runs/server")))
        }
    }
    mods {
        create(rootProject.property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }
}
configurations {
//    common {
//        canBeResolved = true
//        canBeConsumed = false
//    }
//    compileClasspath.extendsFrom(common)
//    runtimeClasspath.extendsFrom(common)
//    runtimeClasspath.extendsFrom(configurations.getByName("shadow"))
//    developmentNeoForge.extendsFrom(common)
//
//    // Files in this configuration will be bundled into your mod using the Shadow plugin.
//    // Don't use the `shadow` configuration from the plugin itself as it's meant for excluding files.
    create("shadowBundle") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

dependencies {
    api("me.shedaniel.cloth:cloth-config-neoforge:${rootProject.property("cloth_config_version")}")

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge-neoforge:${rootProject.property("kotlin_for_forge_version")}") {
        exclude(group = "net.neoforged.fancymodloader", module = "loader")
    }
    shadow("net.java.dev.jna:jna:5.14.0")
    shadow("com.alphacephei:vosk:0.3.45")
}
//accessx {
//    convert("neoforge") {
//        inputFiles.from(project(":common").file("src/main/resources/chatplus.accessWidener"))
//        outputFormat = AT
//    }
//}

tasks.processResources {
    inputs.property("group", rootProject.property("maven_group"))
    inputs.property("version", project.version)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(
            mapOf(
                "group" to rootProject.property("maven_group"),
                "version" to project.version,

                "mod_id" to rootProject.property("mod_id"),
                "min_minecraft_version" to rootProject.property("min_minecraft_version"),
                "kotlin_for_forge_version" to rootProject.property("kotlin_for_forge_version"),
                "cloth_config_version" to rootProject.property("cloth_config_version"),

                "mod_name" to rootProject.property("mod_name"),
                "mod_description" to rootProject.property("mod_description"),
                "mod_authors" to rootProject.property("mod_authors"),
            )
        )
    }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    configurations = listOf(
        project.configurations.getByName("shadowBundle"),
        project.configurations.getByName("shadow")
    )
    archiveClassifier.set("all")
}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.sourcesJar {
    val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.getByName("java") {
    this as AdhocComponentWithVariants
    this.withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}

publishMods {
    println("(${project.name}) Publishing | ${rootProject.property("minecraft_version")} | ${project.name}")
    file.set(tasks.shadowJar.flatMap { it.archiveFile })
    displayName.set("${rootProject.property("mod_name")} ${project.name.uppercaseFirstChar()} v${project.version}")
    val releaseChangeLog = project.ext.get("releaseChangeLog") as? () -> String
    changelog.set(releaseChangeLog?.invoke() ?: "")
    type.set(STABLE)
    modLoaders.add(project.name)

    val mcVersions = "${rootProject.property("supported_minecraft_version")}".split(",")

    val cfToken = System.getenv("CF_TOKEN")
    if (cfToken != null) {
        println("(${project.name}) CF_TOKEN found, publishing to CurseForge")
        curseforge {
            accessToken.set(cfToken)
            projectId.set("1023333")
            minecraftVersions.addAll(mcVersions)
            client.set(true)
            requires("cloth-config", "kotlin-for-forge")
        }
    } else {
        println("(${project.name}) CF_TOKEN not found, not publishing to CurseForge")
    }

    val mrToken = System.getenv("MODRINTH_TOKEN")
    if (mrToken != null) {
        println("(${project.name}) MODRINTH_TOKEN found, publishing to Modrinth")
        modrinth {
            accessToken.set(mrToken)
            projectId.set("cJlZ132G")
            minecraftVersions.addAll(mcVersions)
            environment.set(CLIENT_ONLY)
            requires("9s6osm5g", "ordsPcFz")
        }
    } else {
        println("(${project.name}) MODRINTH_TOKEN not found, not publishing to Modrinth")
    }
}