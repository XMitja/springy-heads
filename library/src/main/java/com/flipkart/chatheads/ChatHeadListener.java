package com.flipkart.chatheads;

import com.flipkart.chatheads.arrangement.ChatHeadArrangement;

import java.io.Serializable;

/**
 * Created by kiran.kumar on 06/05/15.
 */
public interface ChatHeadListener<T> {
    void onChatHeadAdded(T key);
    void onChatHeadRemoved(T key, boolean userTriggered);
    void onChatHeadArrangementChanged(ChatHeadArrangement oldArrangement, ChatHeadArrangement newArrangement);
    void onChatHeadAnimateEnd(ChatHead chatHead);
    void onChatHeadAnimateStart(ChatHead chatHead);
}
