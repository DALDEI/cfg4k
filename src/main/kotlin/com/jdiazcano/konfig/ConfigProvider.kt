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

package com.jdiazcano.konfig

import com.jdiazcano.konfig.parsers.Parser
import com.jdiazcano.konfig.utils.Typable

interface ConfigProvider {
    fun <T: Any> getProperty(name: String, type: Class<T>): T
    fun <T: Any> getProperty(name: String, type: Typable): T
    fun <T: Any> bind(prefix: String, type: Class<T>): T
    fun canParse(type: Class<out Any>): Boolean
    fun reload()

    // These methods must be included because the possibility of registering custom parsers must be there
    fun addParser(type: Class<out Any>, parser: Parser<Any>)
    fun addClassedParser(type: Class<out Any>, parser: Parser<Any>)
    fun addParseredParser(type: Class<out Any>, parser: Parser<Any>)
    fun cancelReload(): Unit?

    fun addReloadListener(listener: () -> Unit)
}