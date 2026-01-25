package com.ebicep.chatplus.accessor;

import com.ebicep.chatplus.hud.ChatRenderContext;

public interface IMixinChatRenderContext {

    ChatRenderContext chatPlus$getChatRenderContext();

    void chatPlus$setChatRenderContext(ChatRenderContext chatRenderContext);

}
