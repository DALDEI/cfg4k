/*
 * Copyright 2015-2016 Javier Díaz-Cano Martín-Albo (javierdiazcanom@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jdiazcano.konfig.bytebuddy

import com.jdiazcano.konfig.ConfigLoader
import com.jdiazcano.konfig.loaders.ReloadStrategy
import com.jdiazcano.konfig.providers.AbstractConfigProvider
import com.jdiazcano.konfig.providers.Providers
import com.jdiazcano.konfig.utils.Typable
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers
import java.lang.reflect.Modifier
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
open class ByteBuddyConfigProvider(
        configLoader: ConfigLoader,
        reloadStrategy: ReloadStrategy? = null
): AbstractConfigProvider(configLoader, reloadStrategy) {

    override fun <T: Any> bind(prefix: String, type: Class<T>): T {
        var subclass = ByteBuddy().subclass(type)
        type.methods.forEach { method ->
            subclass = subclass
                    .defineMethod(method.name, method.returnType, Modifier.PUBLIC)
                    .intercept(FixedValue.value(getProperty<Any>(method.name, object : Typable {
                        override fun getType(): Type {
                            return method.genericReturnType
                        }
                    }) as T))
        }
        return subclass.make().load(javaClass.classLoader).loaded.newInstance()
    }

}

fun Providers.Companion.bytebuddy(loader: ConfigLoader, reloadStrategy: ReloadStrategy? = null) = ByteBuddyConfigProvider(loader, reloadStrategy)