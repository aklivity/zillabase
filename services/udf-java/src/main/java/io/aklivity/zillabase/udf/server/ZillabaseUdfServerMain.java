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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.risingwave.functions.ScalarFunction;
import com.risingwave.functions.TableFunction;
import com.risingwave.functions.UdfServer;
import com.risingwave.functions.UserDefinedFunction;
import com.sun.net.httpserver.HttpServer;

public final class ZillabaseUdfServerMain
{
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("([a-z])([A-Z])");

    public static void main(
        String[] args)
    {
        final Matcher matcher = CLASS_NAME_PATTERN.matcher("");
        final List<URL> urls = new ArrayList<>();

        final List<DiscoveredFunction> discoveredFunctions = new ArrayList<>();

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
                            .replaceAll(m -> m.group(1) + "_" + m.group(2).toLowerCase())
                            .toLowerCase();
                        server.addFunction(name, function);

                        Method evalMethod = findEvalMethod(function.getClass());
                        List<ParameterInfo> paramInfos = new ArrayList<>();
                        String returnType = "void";

                        if (evalMethod != null)
                        {
                            returnType = evalMethod.getReturnType().getSimpleName();

                            for (Parameter p : evalMethod.getParameters())
                            {
                                String paramName = p.getName();
                                String paramType = p.getType().getSimpleName();
                                paramInfos.add(new ParameterInfo(paramName, paramType));
                            }
                        }

                        discoveredFunctions.add(
                            new DiscoveredFunction(name, paramInfos, returnType));
                    }
                }
                catch (Exception | Error ex)
                {
                    System.out.println("Failed to load: " + className);
                }
            }

            startHttpServer(discoveredFunctions);

            server.start();
            server.awaitTermination();
        }
        catch (Exception ex)
        {
            System.out.println("Failed to start UdfServer: " + ex.getMessage());
        }
    }

    private static Method findEvalMethod(
        Class<?> clazz)
    {
        for (Method m : clazz.getDeclaredMethods())
        {
            if ("eval".equals(m.getName()))
            {
                return m;
            }
        }
        return null;
    }


    private static void startHttpServer(
        List<DiscoveredFunction> discoveredFunctions) throws IOException
    {
        HttpServer httpServer = HttpServer.create(new java.net.InetSocketAddress("0.0.0.0", 5001), 0);

        httpServer.createContext("/java/methods", exchange ->
        {
            // Convert the discovered function list to JSON
            String response = toJson(discoveredFunctions);

            byte[] bytes = response.getBytes();
            exchange.sendResponseHeaders(200, bytes.length);

            try (var os = exchange.getResponseBody())
            {
                os.write(bytes);
            }
        });

        httpServer.setExecutor(null);
        httpServer.start();
        System.out.println("HTTP server is listening on port 5001");
    }

    private static String toJson(
        List<DiscoveredFunction> discoveredFunctions)
    {
        String json = "[]";
        try
        {
            ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            json = writer.writeValueAsString(discoveredFunctions);
        }
        catch (Exception e)
        {
            //Ignore
        }

        return json;
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

    private record DiscoveredFunction(
        String name,
        List<ParameterInfo> params,
        String returnType)
    {
    }

    private record ParameterInfo(
        String paramName,
        String paramType)
    {
    }
}
