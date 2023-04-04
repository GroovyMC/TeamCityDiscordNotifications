/*
 * Copyright (c) 2023 Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.teamcity.discord;

import com.google.common.base.Strings;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.BuildTypeIdentity;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscordNotificationBuildFeature extends BuildFeature {
    public static final String ID = "discordNotification";

    private final String myEditUrl;
    public DiscordNotificationBuildFeature(@NotNull final PluginDescriptor descriptor, @NotNull final WebControllerManager web) {
        final String jsp = descriptor.getPluginResourcesPath("editDiscordNotifierFeature.jsp");
        final String html = descriptor.getPluginResourcesPath("editDiscordNotifierFeature.html");

        web.registerController(html, new BaseController() {
            @Override
            protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws Exception {
                return new ModelAndView(jsp);
            }
        });

        myEditUrl = html;
    }

    @NotNull
    @Override
    public String getType() {
        return ID;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Discord Notification";
    }

    @Nullable
    @Override
    public String getEditParametersUrl() {
        return myEditUrl;
    }

    @Override
    public boolean isMultipleFeaturesPerBuildTypeAllowed() {
        return false;
    }

    @NotNull
    @Override
    public String describeParameters(@NotNull Map<String, String> params) {
        return "Sends a Discord notification when the build status changes.";
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultParameters() {
        return CollectionsUtil.asMap("discordNotifier.url", "");
    }

    @Nullable
    @Override
    public PropertiesProcessor getParametersProcessor(@NotNull BuildTypeIdentity buildTypeOrTemplate) {
        return properties ->  {
            List<InvalidProperty> errors = new ArrayList<>();
            String url = properties.get("discordNotifier.url");
            if (Strings.isNullOrEmpty(url)) {
                errors.add(new InvalidProperty("discordNotifier.url", "Webhook URL should be specified."));
            }
            return errors;
        };
    }
}
