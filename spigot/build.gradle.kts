dependencies {

    api(project(":common"))

    compileOnly("org.purpurmc.purpur:purpur-api:1.20.4-R0.1-SNAPSHOT") {
        exclude(group = "net.kyori", module = "adventure-api")
    }
    api("net.kyori:adventure-api:5.1.1")
    compileOnly("com.mojang:authlib:3.11.50")
}
