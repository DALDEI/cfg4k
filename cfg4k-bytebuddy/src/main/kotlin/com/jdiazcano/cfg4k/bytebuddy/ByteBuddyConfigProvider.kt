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

package com.jdiazcano.cfg4k.bytebuddy

import com.jdiazcano.cfg4k.binders.*
import com.jdiazcano.cfg4k.loaders.ConfigLoader
import com.jdiazcano.cfg4k.providers.ConfigProvider
import com.jdiazcano.cfg4k.reloadstrategies.ReloadStrategy
import com.jdiazcano.cfg4k.parsers.Parsers.isParseable
import com.jdiazcano.cfg4k.providers.DefaultConfigProvider
import com.jdiazcano.cfg4k.providers.Providers
import com.jdiazcano.cfg4k.utils.SettingNotFound
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.implementation.*
import java.lang.reflect.Modifier
import net.bytebuddy.implementation.bind.annotation.RuntimeType
import net.bytebuddy.matcher.ElementMatchers.isDeclaredBy
import net.bytebuddy.matcher.ElementMatchers.not
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.kotlinFunction


@Suppress("UNCHECKED_CAST")
open class ByteBuddyConfigProvider(
        configLoader: ConfigLoader,
        reloadStrategy: ReloadStrategy? = null
): DefaultConfigProvider(configLoader, reloadStrategy, ByteBuddyBinder())

@Suppress("UNCHECKED_CAST")
class ByteBuddyBinder : Binder {
    override fun <T : Any> bind(provider: ConfigProvider, prefix: String, type: Class<T>): T {
        var subclass = ByteBuddy().subclass(type, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
        type.methods.forEach { method ->

            val returnType = method.genericReturnType
            val methodName = method.name
            val name = getPropertyName(methodName)

            val kotlinClass = method.declaringClass.kotlin
            val isNullable = kotlinClass.isMethodNullable(method, name)

            val value: (Boolean) -> T? = { nullable ->
                var returning: T?
                if (method.returnType.isParseable()) {
                    if (nullable) {
                        val value = provider.getOrNull<T?>(prefix(prefix, name), returnType)
                        if (value != null) {
                            returning = value
                        } else {
                            try {
                                returning = kotlinClass.getDefaultMethod(method.name)?.invoke(kotlinClass.objectInstance, kotlinClass.objectInstance) as T?
                            } catch (e: Exception) {
                                returning = null
                            }
                        }
                    } else {
                        try {
                            returning = provider.get(prefix(prefix, name), returnType)
                        } catch (notFound: SettingNotFound) {
                            try {
                                returning = kotlinClass.getDefaultMethod(method.name)?.invoke(kotlinClass.objectInstance, kotlinClass.objectInstance) as T?
                            } catch (e: Exception) {
                                throw notFound
                            }
                        }
                    }
                } else {
                    returning = provider.bind(prefix(prefix, name), method.returnType) as T
                }
                returning
            }
            subclass = subclass
                    .defineMethod(method.name, method.returnType, Modifier.PUBLIC)
                    .intercept(MethodDelegation
                            .withEmptyConfiguration()
                            .filter(not(isDeclaredBy(Any::class.java)))
                            .to(object : Any() { @RuntimeType fun delegate() = value(isNullable) }))
        }
        val instance = subclass.make().load(javaClass.classLoader).loaded.newInstance()
        return instance
    }
}

fun Providers.bytebuddy(loader: ConfigLoader, reloadStrategy: ReloadStrategy? = null) = ByteBuddyConfigProvider(loader, reloadStrategy)