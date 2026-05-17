package net.momirealms

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

open class RelocationExtension {
    open fun applyCommon(task: ShadowJar) {
        with(task) {
            val libs = "net.momirealms.craftengine.libraries"
            relocate("net.kyori", libs)
            relocate("net.momirealms.sparrow.reflection", "$libs.reflection")
            relocate("net.momirealms.sparrow.nbt", "$libs.nbt")
            relocate("net.momirealms.antigrieflib", "$libs.antigrieflib")
            relocate("cn.gtemc.itembridge", "$libs.itembridge")
            relocate("cn.gtemc.levelerbridge", "$libs.levelerbridge")
            relocate("org.incendo", libs)
            relocate("dev.dejvokep", libs)
            relocate("org.bstats", "$libs.bstats")
            relocate("com.github.benmanes.caffeine", "$libs.caffeine")
            relocate("com.ezylang.evalex", "$libs.evalex")
            relocate("net.bytebuddy", "$libs.bytebuddy")
            relocate("org.snakeyaml", "$libs.snakeyaml")
            relocate("org.ahocorasick", "$libs.ahocorasick")
            relocate("net.jpountz", "$libs.jpountz")
            relocate("software.amazon.awssdk", "$libs.awssdk")
            relocate("software.amazon.eventstream", "$libs.eventstream")
            relocate("com.google.common.jimfs", "$libs.jimfs")
            relocate("org.apache.commons", "$libs.commons")
            relocate("io.leangen.geantyref", "$libs.geantyref")
            relocate("ca.spottedleaf.concurrentutil", "$libs.concurrentutil")
            relocate("io.netty.handler.codec.http", "$libs.netty.handler.codec.http")
            relocate("io.netty.handler.codec.rtsp", "$libs.netty.handler.codec.rtsp")
            relocate("io.netty.handler.codec.spdy", "$libs.netty.handler.codec.spdy")
            relocate("io.netty.handler.codec.http2", "$libs.netty.handler.codec.http2")
            relocate("io.github.bucket4j", "$libs.bucket4j")
            mergeServiceFiles()
        }
    }
}