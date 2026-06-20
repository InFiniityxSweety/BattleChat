import org.gradle.api.attributes.Attribute

plugins {
    id("multiloader-common")
    id("net.fabricmc.fabric-loom")
}

loom {
    accessWidenerPath.set(file(project(":common").file("src/main/resources/${rootProject.property("mod_id")}.accesswidener")))
}

dependencies {
    minecraft ("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")

    compileOnly("me.shedaniel.cloth:cloth-config:${rootProject.property("cloth_config_version")}") {
        exclude(group= "net.fabricmc.fabric-api")
        exclude(group= "net.fabricmc:fabric-loader")
    }
}

val loaderAttribute: Attribute<String> =
    Attribute.of("io.github.mcgradleconventions.loader", String::class.java)

listOf("apiElements", "runtimeElements").forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, "common")
        }
    }
}

sourceSets.configureEach {
    val compileCp = configurations.named(compileClasspathConfigurationName)
    val runtimeCp = configurations.named(runtimeClasspathConfigurationName)

    compileCp.configure {
        attributes {
            attribute(loaderAttribute, "common")
        }
    }

    runtimeCp.configure {
        attributes {
            attribute(loaderAttribute, "common")
        }
    }
}