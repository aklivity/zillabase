/*
 * Copyright 2024 Aklivity Inc
 *
 * Licensed under the Aklivity Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 *   https://www.aklivity.io/aklivity-community-license/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.aklivity.zillabase.udf.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.risingwave.functions.ScalarFunction;
import com.risingwave.functions.TableFunction;
import com.risingwave.functions.UdfServer;
import com.risingwave.functions.UserDefinedFunction;

public final class ZillabaseUdfServerMain
{
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("([a-z])([A-Z])");

    public static void main(String[] args)
    {
        final Matcher matcher = CLASS_NAME_PATTERN.matcher("");
        final List<URL> urls = new ArrayList<>();

        try (var server = new UdfServer("0.0.0.0", 8815))
        {
            List<String> classNames = fetchClasses(urls);
            for (String className : classNames)
            {
                try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0])))
                {
                    Class<?> clazz = classLoader.loadClass(className);
                    UserDefinedFunction function = null;

                    if (!Modifier.isAbstract(clazz.getModifiers()))
                    {
                        if (ScalarFunction.class.isAssignableFrom(clazz))
                        {
                            function = (ScalarFunction) clazz.getDeclaredConstructor().newInstance();

                        }
                        else if (TableFunction.class.isAssignableFrom(clazz))
                        {
                            function = (TableFunction) clazz.getDeclaredConstructor().newInstance();
                        }
                    }

                    if (function != null)
                    {
                        String simpleName = function.getClass().getSimpleName();
                        String name = matcher.reset(simpleName)
                            .replaceAll(m -> m.group(1) + "_" + m.group(2).toLowerCase());
                        server.addFunction(name.toLowerCase(), function);
                    }
                }
                catch (Exception | Error ex)
                {
                    System.err.println("Failed to load: " + className);
                }
            }

            server.start();
            server.awaitTermination();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private static List<String> fetchClasses(
        List<URL> urls)
    {
        List<String> classNames = new ArrayList<>();
        String classpath = System.getProperty("java.class.path");
        if (classpath != null)
        {

            String[] paths = classpath.split(File.pathSeparator);
            for (String path : paths)
            {
                if (path.startsWith("/opt/udf/lib"))
                {
                    File file = new File(path);
                    if (file.getName().endsWith(".jar"))
                    {
                        try (JarFile jar = new JarFile(file))
                        {
                            jar.stream()
                                .filter(entry -> entry.getName().endsWith(".class"))
                                .forEach(entry ->
                                {
                                    String className = entry.getName()
                                        .replace('/', '.')
                                        .replace(".class", "");

                                    classNames.add(className);
                                });
                            urls.add(file.toURI().toURL());
                        }
                        catch (IOException ex)
                        {
                            ex.printStackTrace();
                        }

                    }
                }
            }
        }
        return classNames.stream().distinct().collect(Collectors.toList());
    }

    private ZillabaseUdfServerMain()
    {
    }
}
