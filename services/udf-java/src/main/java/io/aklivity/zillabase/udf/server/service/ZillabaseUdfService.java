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
package io.aklivity.zillabase.udf.server.service;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
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

import io.aklivity.zillabase.udf.server.model.DiscoveredFunction;
import io.aklivity.zillabase.udf.server.model.FunctionMetadata;
import io.aklivity.zillabase.udf.server.model.ParameterInfo;

public class ZillabaseUdfService
{
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("([a-z])([A-Z])");

    private static final int HTTP_PORT = 5001;

    public void run(
        UdfServer server) throws IOException
    {
        final List<URL> urls = new ArrayList<>();
        final List<String> classNames = fetchClasses(urls);

        List<DiscoveredFunction> discoveredFunctions =
            discoverAndRegisterUdfs(server, urls, classNames);

        startHttpServer(discoveredFunctions);
    }

    private List<String> fetchClasses(
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
                                    String clsName = entry.getName()
                                                          .replace('/', '.')
                                                          .replace(".class", "");
                                    classNames.add(clsName);
                                });
                            urls.add(file.toURI().toURL());
                        }
                        catch (IOException ex)
                        {
                            System.out.println("Failed to read jar file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }
        return classNames.stream().distinct().collect(Collectors.toList());
    }

    private List<DiscoveredFunction> discoverAndRegisterUdfs(
        UdfServer server,
        List<URL> urls,
        List<String> classNames)
    {
        final List<DiscoveredFunction> discoveredFunctions = new ArrayList<>();
        final Matcher matcher = CLASS_NAME_PATTERN.matcher("");

        for (String className : classNames)
        {
            try (URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0])))
            {
                Class<?> clazz = classLoader.loadClass(className);

                UserDefinedFunction function = instantiateFunctionIfValid(clazz);
                if (function != null)
                {
                    final String simpleName = function.getClass().getSimpleName();
                    final String functionName = toSnakeCase(simpleName, matcher);

                    server.addFunction(functionName, function);

                    final Method evalMethod = findEvalMethod(function.getClass());
                    final FunctionMetadata metadata = (evalMethod != null)
                        ? evalMethodMetadata(evalMethod)
                        : new FunctionMetadata(new ArrayList<>(), "void");

                    discoveredFunctions.add(
                        new DiscoveredFunction(
                            functionName,
                            metadata.params(),
                            metadata.returnType()
                        )
                    );
                }
            }
            catch (Exception | Error ex)
            {
                System.out.println("Failed to load: " + className);
            }
        }
        return discoveredFunctions;
    }

    private UserDefinedFunction instantiateFunctionIfValid(
        Class<?> clazz) throws Exception
    {
        if (!Modifier.isAbstract(clazz.getModifiers()))
        {
            if (ScalarFunction.class.isAssignableFrom(clazz))
            {
                return (ScalarFunction) clazz.getDeclaredConstructor().newInstance();
            }
            else if (TableFunction.class.isAssignableFrom(clazz))
            {
                return (TableFunction) clazz.getDeclaredConstructor().newInstance();
            }
        }
        return null;
    }


    private String toSnakeCase(
        String simpleName,
        Matcher matcher)
    {
        return matcher.reset(simpleName)
            .replaceAll(m -> m.group(1) + "_" + m.group(2).toLowerCase())
            .toLowerCase();
    }

    private Method findEvalMethod(
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

    private FunctionMetadata evalMethodMetadata(
        Method evalMethod)
    {
        String returnType = evalMethod.getReturnType().getSimpleName();
        List<ParameterInfo> params = new ArrayList<>();

        for (Parameter p : evalMethod.getParameters())
        {
            String paramName = p.getName();
            String paramType = p.getType().getSimpleName();
            params.add(new ParameterInfo(paramName, paramType));
        }
        return new FunctionMetadata(params, returnType);
    }


    private void startHttpServer(
        List<DiscoveredFunction> discoveredFunctions) throws IOException
    {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", HTTP_PORT), 0);

        httpServer.createContext("/java/methods", exchange ->
        {
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
        System.out.println("HTTP server is listening on port " + HTTP_PORT);
    }

    private String toJson(
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
}
