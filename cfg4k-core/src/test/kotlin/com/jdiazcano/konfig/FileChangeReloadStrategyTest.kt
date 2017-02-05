package com.jdiazcano.konfig

import com.jdiazcano.konfig.binders.ProxyBinder
import com.jdiazcano.konfig.loaders.PropertyConfigLoader
import com.jdiazcano.konfig.providers.DefaultConfigProvider
import com.jdiazcano.konfig.providers.getProperty
import com.jdiazcano.konfig.reloadstrategies.FileChangeReloadStrategy
import com.winterbe.expekt.should
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.nio.file.Paths

class FileChangeReloadStrategyTest : Spek({
    describe("a provider with a filechange reloader") {
        val file = Paths.get("test.properties")
        file.toFile().writeText("foo=bar")
        val provider = DefaultConfigProvider(
                PropertyConfigLoader(file.toUri().toURL()),
                FileChangeReloadStrategy(file),
                ProxyBinder()
        )

        it("should have this config loaded") {
            provider.getProperty<String>("foo").should.be.equal("bar")
        }

        it("should update the configuration") {
            Thread.sleep(5000) // We need to wait a little bit for the event watcher to be triggered
            file.toFile().writeText("foo=bar2")
            Thread.sleep(5000) // Another wait for that
            provider.getProperty<String>("foo").should.be.equal("bar2")
            provider.cancelReload()
            file.toFile().writeText("foo=bar3")
            Thread.sleep(5000) // Another wait for that
            provider.getProperty<String>("foo").should.be.equal("bar2") // We hav ecanceled the reload so no more reloads
            file.toFile().deleteOnExit()
        }
    }
})