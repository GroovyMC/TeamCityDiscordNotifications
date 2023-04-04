/*
 * Copyright (c) 2023 Matyrobbrt
 * SPDX-License-Identifier: MIT
 */

package com.matyrobbrt.teamcity.discord;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.HTTPRequestBuilder;
import jetbrains.buildServer.util.http.HttpMethod;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;

public class NotificationAdapter extends BuildServerAdapter {
    private final @NotNull HTTPRequestBuilder.RequestHandler requestHandler;
    private final URI rootUrl;
    public NotificationAdapter(@NotNull SBuildServer server, @NotNull EventDispatcher<BuildServerListener> events,
//                                         @NotNull BuildHistory buildHistory,
//                                         @NotNull BuildsManager buildsManager,
//                                         @NotNull BuildPromotionManager buildPromotionManager,
//                                         @NotNull ServerResponsibility serverResponsibility,
//                     MultiNodeTasks multiNodeTasks,
                               @NotNull HTTPRequestBuilder.RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        events.addListener(this);
        this.rootUrl = URI.create(server.getRootUrl());
    }

    @Override
    public void buildStarted(@NotNull SRunningBuild build) {
        if (build.getBuildType() == null) return;
        final Collection<SBuildFeatureDescriptor> features = build.getBuildFeaturesOfType(DiscordNotificationBuildFeature.ID);
        if (features.isEmpty()) return;

        for (final SBuildFeatureDescriptor feature : features) {
            try {
                final WebhookMessageBuilder message = new WebhookMessageBuilder()
                        .setAvatarUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/TeamCity_Icon.svg/64px-TeamCity_Icon.svg.png")
                        .setUsername("TeamCity")
                        .setContent(String.format("%s: [Build #%s Started](%s)", build.getBuildType().getProject().getName(), build.getBuildNumber(), computeBuildLink(build)));

                build.getChanges(SelectPrevBuildPolicy.SINCE_LAST_BUILD, true).forEach(change -> message.append(String.format("\n  - %s", change.getDescription())));

                final String url = feature.getParameters().get("discordNotifier.url");

                requestHandler.doRequest(new HTTPRequestBuilder(url)
                        .withData(buildWebhookMessage(message.build()).getBytes(StandardCharsets.UTF_8))
                        .withMethod(HttpMethod.POST)
                        .withHeader("Content-Type", "application/json").build());
            } catch (Exception e) {
                System.out.println("Whoops... " + e);
            }
        }
    }

    @Override
    public void buildFinished(@NotNull SRunningBuild build) {
        if (build.getBuildType() == null) return;
        final Collection<SBuildFeatureDescriptor> features = build.getBuildFeaturesOfType(DiscordNotificationBuildFeature.ID);
        if (features.isEmpty()) return;

        for (final SBuildFeatureDescriptor feature : features) {
            try {
                final WebhookMessageBuilder message = new WebhookMessageBuilder()
                        .setAvatarUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/2/29/TeamCity_Icon.svg/64px-TeamCity_Icon.svg.png")
                        .setUsername("TeamCity");

                final boolean failed = build.getBuildStatus().isFailed();

                final WebhookEmbedBuilder embed = new WebhookEmbedBuilder()
                        .setAuthor(new WebhookEmbed.EmbedAuthor(build.getBuildType().getProject().getName(), null, rootUrl.resolve("/project/" + build.getBuildType().getProject().getExternalId()).toString()))
                        .setTimestamp(Instant.now())
                        .setColor(failed ? 0xff0000 : 0x00ff00)
                        .setDescription(String.format("**[Build %s](%s)**", failed ? "failed" : "succeeded", computeBuildLink(build)))
                        .addField(new WebhookEmbed.EmbedField(true, "Build number", build.getBuildNumber()));

                if (build.getBranch() != null) {
                    embed.addField(new WebhookEmbed.EmbedField(true, "Build branch", build.getBranch().getDisplayName()));
                }

                final String url = feature.getParameters().get("discordNotifier.url");
                requestHandler.doRequest(new HTTPRequestBuilder(url)
                        .withData(buildWebhookMessage(message.addEmbeds(embed.build()).build()).getBytes(StandardCharsets.UTF_8))
                        .withMethod(HttpMethod.POST)
                        .withHeader("Content-Type", "application/json").build());
            } catch (Exception e) {
                System.out.println("Whoops... " + e);
            }
        }
    }

    private String computeBuildLink(SBuild build) {
        return rootUrl.resolve("/buildConfiguration/" + build.getBuildType().getExternalId() + "/" + build.getBuildId()).toString();
    }

    public static String buildWebhookMessage(WebhookMessage message) {
        final JSONObject payload = new JSONObject();
        payload.put("content", message.getContent());
        if (!message.getEmbeds().isEmpty()) {
            final JSONArray array = new JSONArray();
            for (WebhookEmbed embed : message.getEmbeds()) {
                array.put(embed.reduced());
            }
            payload.put("embeds", array);
        } else {
            payload.put("embeds", new JSONArray());
        }
        if (message.getAvatarUrl() != null)
            payload.put("avatar_url", message.getAvatarUrl());
        if (message.getUsername() != null)
            payload.put("username", message.getUsername());
        payload.put("tts", message.isTTS());
        final JSONObject allowedMentions = new JSONObject();
        allowedMentions.put("parse", new JSONObject());
        payload.put("allowed_mentions", allowedMentions);
        payload.put("flags", message.getFlags());
        return payload.toString();
    }

}
