package com.example.android_project03;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class BluetoothService extends Service {
	// UUID pour protocole SPP
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");
	// Classe de l'adaptateur Bluetooth
	private BluetoothAdapter mAdapter; //
	// Handlers
	private Handler mHandler; // Handler de communication avec "UI Activity"
	private ConnectThread mConnectThread; // Thread de gestion de la mise en
											// connexion
	private ConnectedThread mConnectedThread;// Thread de gestion de la
												// connexion
	// Attribut de l'état actuel de la connexion
	private int mState;
	// Valeurs possibles de l'attribut "mState"
	public static final int STATE_NONE = 0; // Aucun traitement en cours
	public static final int STATE_LISTEN = 1; // Ecoute d'une connexion entrante
												// (non utilisé)
	public static final int STATE_CONNECTING = 2; // Etablissement d'une
													// connexion sortante
	public static final int STATE_CONNECTED = 3; // La connexion est établie

	/**
	 * Constructeur. Préparation d'une nouvelle connexion Bluetooth
	 * 
	 * @param context
	 *            Le contexte de l'activité UI
	 * @param handler
	 *            Le "Handler" utilisé pour transmettre des messages de retour
	 *            vers l'activité principale UI
	 */
	public BluetoothService(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();// Instanciation de la
														// classe locale
														// "mAdapter"
		mState = STATE_NONE; // Aucun traitement en cours
		mHandler = handler; // Instanciation du Handler local
	}

	/**
	 * "Setter" de l'état courant de la connexion
	 * 
	 * @param state
	 *            Etat de la connexion
	 */
	private synchronized void setState(int state) {
		mState = state;
		// Transmission de l'état vers l'activité principale UI
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1)
				.sendToTarget();
	}

	/**
	 * "Getter" qui renvoit l'état actuel de la connexion
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * Démarrer le "ConnectThread" pour établir une connexion avec le
	 * périphérique BT
	 * 
	 * @param device
	 *            Le "BluetoothDevice" avec lequel on doit se connecter
	 */
	public synchronized void connect(BluetoothDevice device) {
		// Arrêter d'abord tous les thread de connexion en cours
		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		// Démarrer le thread de connexion avec le "BluetoothDevice" donné en
		// paramètre
		mConnectThread = new ConnectThread(device); // Instanciation du thread
		mConnectThread.start(); // Démarrer le thread
		setState(STATE_CONNECTING); // Nouvel état de la connexion transmis à
									// l'UI

	}

	/**
	 * Démarrer le "ConnectedThread" pour maintenir une connexion avec le
	 * périphérique BT
	 * 
	 * @param socket
	 *            Le "BluetoothSocket" de la connexion en cours
	 * @param device
	 *            Le "BluetoothDevice" du périphérique connecté
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		// Arrêt du thread "mConnectThread" de mise en connexion
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		// Arrêt de l'éventuel thread de gestion d'une connexion établie
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		// Instanciation du thread de maintien de la connexion
		mConnectedThread = new ConnectedThread(socket);
		// Démarrer ce thread
		mConnectedThread.start();
		// Préparation du message qui sera affecté du nom du périphérique
		// Bluetooth
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		// Instanciation et affectation d'un "Bundle" avec ce nom
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		// Renvoyer le nom du périphérique vers l'activité principale
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		// Nouvel état de la connexion transmis à l'UI
		setState(STATE_CONNECTED);
	}

	/**
	 * Methode d'arrêt de tous les threads
	 */
	public synchronized void stop() {
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		setState(STATE_NONE); // Nouvel état de connexion
	}

	/**
	 * Method used to write !
	 * 
	 * @param out
	 *            Une chaine de caractère! string!
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(String out) {
		// Création d'un objet "ConnectedThread" temporaire
		ConnectedThread r;
		// Synchroniation de la copie du ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			// Uniquement en mode connecté
			r = mConnectedThread; // Instanciation de l'objet temporaire
		}
		// Ecriture effective
		r.sendTestString(out);
	}

	/**
	 * Méthode appelée quand une connexion n'a pas pu être établie pour en
	 * notifier l'activité principale UI
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN); // Nouvel état de la connexion
		// Préparation et renvoi d'un message de perte vers l'activité
		// principale
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Bluetooth connection failed !");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Méthode appelée quand une connexion est rompue pour en notifier
	 * l'activité principale UI
	 */
	public void connectionLost() {
		setState(STATE_LISTEN);// Nouvel état de la connexion
		// Préparation et renvoi d'un message de rupture vers l'activité
		// principale
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Bluetooth connection lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Ce thread est actif jusqu'à la connexion effective avec le périphérique
	 * BT ou après un délai pré-déterminé.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket; // Interface de gestion d'une
												// liaison BT RFCOMM
		private final BluetoothDevice mmDevice; // Objet associé au périphérique
												// BT externe

		// Constructeur du thread appelé ds la méthode "connect"
		// L'argument "device" est affecté avec le périphériques BT choisi

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmpSocket = null; // Classe temporaire de gestion
												// des canaux
												// d'écriture/lecture Bluetooth
			// Instanciation de la classe "BluetoothSocket"
			try {// Création d'un socket RFCOM avec l'UUID donné en argument
				tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
			}
			// Affectation du socket définitif avec le socket temporaire
			mmSocket = tmpSocket;
		}

		// Méthode "run" du "ConnectThread"
		// Appelée régulièrement par le gestionnaire de tâche jusqu'à
		// l'établissement
		// de la connexion
		public void run() {
			setName("ConnectThread");
			// Arrêt de la découverte car cette fonction est gourmande en
			// énergie
			if (mAdapter.isDiscovering())
				mAdapter.cancelDiscovery();
			// Etablir une connexion avec le "BluetoothSocket"
			try {
				// Cet appel est bloquant et ne revient qu'après l'établissement
				// de la
				// connexion ou en cas d'exception
				mmSocket.connect(); // Connexion effective. Le code PIN sera
									// demandé
			} catch (IOException e) {// En cas d'exceptions : connexion BT
										// impossible
				connectionFailed();
				try {
					mmSocket.close(); // Fermeture du "socket"
				} catch (IOException e2) {
				}
				return;
			}
			// Annulation synchronisée du ConnectThread
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}
			// Connexion établie : démarrer le "ConnectedThread"
			connected(mmSocket, mmDevice);
		}

		// Méthode de fermeture de ce thread
		public void cancel() {
			try {
				mmSocket.close(); // Fermeture du "Socket"
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Ce thread est actif tant que la connexion avec le périphérique BT est
	 * établie Elle gère les transmissions entrantes et sortantes
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket; // Interface de gestion d'une
												// liaison BT RFCOMM
		@SuppressWarnings("unused")
		private final InputStream mmInStream; // Classe de gestion du flux
												// entrant
		private OutputStream mmOutStream; // Classe de gestion du flux
											// sortant

		// Constructeur du thread appelé ds la méthode "connected"
		// L'argument "socket" est affecté pour la connexion en cours

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null; // Classes temporaires de gestion des flux
			OutputStream tmpOut = null;
			// Instanciation des classes temporaires de gestion des flux entrant
			// et sortant
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}
			mmInStream = tmpIn; // Instanciation des classes définitives si pas
								// d'exception
			mmOutStream = tmpOut;
		}

		// Méthode "run" du "ConnectedThread"
		// Appelée régulièrement par le gestionnaire de tâche jusqu'à la rupture
		// de la connexion
		public void run() {
			// Arreter la découverte si besoin
			mAdapter.cancelDiscovery();

			try {
				// se connecter sans arrêt à la socket
				mmSocket.connect();
			} catch (IOException e) {
				e.printStackTrace();

			}

			try {
				// envoyer des données
				mmOutStream = mmSocket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Method used to write
		public void sendTestString(String s) {
			try {
				mmOutStream.write(s.getBytes());
				mmOutStream.flush(); // <-- Try flush to force sending data in
										// buffer

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Methode pour fermer ce thread
		public void cancel() {
			try {
				mmSocket.close(); // Fermeture du socket
			} catch (IOException e) {
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}