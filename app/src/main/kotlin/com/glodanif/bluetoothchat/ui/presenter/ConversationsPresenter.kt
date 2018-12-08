package com.glodanif.bluetoothchat.ui.presenter

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.glodanif.bluetoothchat.data.entity.ChatMessage
import com.glodanif.bluetoothchat.data.entity.Conversation
import com.glodanif.bluetoothchat.data.model.*
import com.glodanif.bluetoothchat.domain.interactor.GetConversationsInteractor
import com.glodanif.bluetoothchat.domain.interactor.GetProfileInteractor
import com.glodanif.bluetoothchat.domain.interactor.RemoveConversationInteractor
import com.glodanif.bluetoothchat.ui.router.ConversationsRouter
import com.glodanif.bluetoothchat.ui.view.ConversationsView
import com.glodanif.bluetoothchat.ui.viewmodel.ConversationViewModel
import com.glodanif.bluetoothchat.ui.viewmodel.converter.ConversationConverter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConversationsPresenter(private val view: ConversationsView,
                             private val router: ConversationsRouter,
                             private val getProfileInteractor: GetProfileInteractor,
                             private val getConversationsInteractor: GetConversationsInteractor,
                             private val removeConversationInteractor: RemoveConversationInteractor,
                             private val connection: BluetoothConnector,
                             private val converter: ConversationConverter
) : LifecycleObserver {

    private val prepareListener = object : OnPrepareListener {

        override fun onPrepared() {

            connection.addOnConnectListener(connectionListener)
            connection.addOnMessageListener(messageListener)

            loadConversations()

            val device = connection.getCurrentConversation()

            if (device != null && connection.isPending()) {
                view.notifyAboutConnectedDevice(converter.transform(device))
            } else {
                view.hideActions()
            }
        }

        override fun onError() {
            releaseConnection()
        }
    }

    private val connectionListener = object : OnConnectionListener {

        override fun onConnected(device: BluetoothDevice) {

        }

        override fun onConnectionWithdrawn() {
            view.hideActions()
        }

        override fun onConnectionDestroyed() {
            view.showServiceDestroyed()
        }

        override fun onConnectionAccepted() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
        }

        override fun onConnectionRejected() {

        }

        override fun onConnectedIn(conversation: Conversation) {
            view.notifyAboutConnectedDevice(converter.transform(conversation))
        }

        override fun onConnectedOut(conversation: Conversation) {
            router.redirectToChat(converter.transform(conversation))
        }

        override fun onConnecting() {

        }

        override fun onConnectionLost() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
            view.hideActions()
        }

        override fun onConnectionFailed() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
            view.hideActions()
        }

        override fun onDisconnected() {
            view.refreshList(connection.getCurrentConversation()?.deviceAddress)
            view.hideActions()
        }
    }

    private val messageListener = object : SimpleOnMessageListener() {

        override fun onMessageReceived(message: ChatMessage) {
            loadConversations()
        }

        override fun onMessageSent(message: ChatMessage) {
            loadConversations()
        }
    }

    fun loadConversations() {

        getConversationsInteractor.execute(Unit,
                onResult = { conversations ->
                    if (conversations.isEmpty()) {
                        view.showNoConversations()
                    } else {
                        val connectedDevice = if (connection.isConnected())
                            connection.getCurrentConversation()?.deviceAddress else null
                        view.showConversations(converter.transform(conversations), connectedDevice)
                    }
                })
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun loadUserProfile() {

        getProfileInteractor.execute(Unit,
                onResult = { profile ->
                    view.showUserProfile(profile.name, profile.color)
                }
        )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun prepareConnection() {

        view.dismissConversationNotification()

        connection.addOnPrepareListener(prepareListener)

        if (connection.isConnectionPrepared()) {

            with(connection) {
                addOnConnectListener(connectionListener)
                addOnMessageListener(messageListener)
            }

            loadConversations()

            val device = connection.getCurrentConversation()

            if (device != null && connection.isPending()) {
                view.notifyAboutConnectedDevice(converter.transform(device))
            } else {
                view.hideActions()
            }
        } else {
            connection.prepare()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun releaseConnection() {
        with(connection) {
            removeOnPrepareListener(prepareListener)
            removeOnConnectListener(connectionListener)
            removeOnMessageListener(messageListener)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        getProfileInteractor.cancel()
        getConversationsInteractor.cancel()
        removeConversationInteractor.cancel()
    }

    fun startChat(conversation: ConversationViewModel) {
        connection.acceptConnection()
        view.hideActions()
        router.redirectToChat(conversation)
    }

    fun rejectConnection() {
        view.hideActions()
        connection.rejectConnection()
    }

    fun removeConversation(address: String) {
        connection.sendDisconnectRequest()
        removeConversationInteractor.execute(address)
        view.removeFromShortcuts(address)
        loadConversations()
    }

    fun disconnect() {
        connection.sendDisconnectRequest()
        loadConversations()
    }

    fun onProfileClick() {
        router.redirectToProfile()
    }

    fun onReceivedImagesClick() {
        router.redirectToReceivedImages()
    }

    fun onSettingsClick() {
        router.redirectToSettings()
    }

    fun onAboutClick() {
        router.redirectToAbout()
    }
}
