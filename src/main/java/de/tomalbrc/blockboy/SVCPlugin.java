package de.tomalbrc.blockboy;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;

public class SVCPlugin implements VoicechatPlugin {
    public static VoicechatServerApi API;

    @Override
    public String getPluginId() {
        return "blockboy";
    }

    @Override
    public void initialize(VoicechatApi api) {
        API = (VoicechatServerApi) api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
    }
}
