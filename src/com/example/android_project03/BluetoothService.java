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
	// Attribut de l'�tat actuel de la connexion
	private int mState;
	// Valeurs possibles de l'attribut "mState"
	public static final int STATE_NONE = 0; // Aucun traitement en cours
	public static final int STATE_LISTEN = 1; // Ecoute d'une connexion entrante
												// (non utilis�)
	public static final int STATE_CONNECTING = 2; // Etablissement d'une
													// connexion sortante
	public static final int STATE_CONNECTED = 3; // La connexion est �tablie

	/**
	 * Constructeur. Pr�paration d'une nouvelle connexion Bluetooth
	 * 
	 * @param context
	 *            Le contexte de l'activit� UI
	 * @param handler
	 *            Le "Handler" utilis� pour transmettre des messages de retour
	 *            vers l'activit� principale UI
	 */
	public BluetoothService(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();// Instanciation de la
														// classe locale
														// "mAdapter"
		mState = STATE_NONE; // Aucun traitement en cours
		mHandler = handler; // Instanciation du Handler local
	}

	/**
	 * "Setter" de l'�tat courant de la connexion
	 * 
	 * @param state
	 *            Etat de la connexion
	 */
	private synchronized void setState(int state) {
		mState = state;
		// Transmission de l'�tat vers l'activit� principale UI
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1)
				.sendToTarget();
	}

	/**
	 * "Getter" qui renvoit l'�tat actuel de la connexion
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * D�marrer le "ConnectThread" pour �tablir une connexion avec le
	 * p�riph�rique BT
	 * 
	 * @param device
	 *            Le "BluetoothDevice" avec lequel on doit se connecter
	 */
	public synchronized void connect(BluetoothDevice device) {
		// Arr�ter d'abord tous les thread de connexion en cours
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
		// D�marrer le thread de connexion avec le "BluetoothDevice" donn� en
		// param�tre
		mConnectThread = new ConnectThread(device); // Instanciation du thread
		mConnectThread.start(); // D�marrer le thread
		setState(STATE_CONNECTING); // Nouvel �tat de la connexion transmis �
									// l'UI

	}

	/**
	 * D�marrer le "ConnectedThread" pour maintenir une connexion avec le
	 * p�riph�rique BT
	 * 
	 * @param socket
	 *            Le "BluetoothSocket" de la connexion en cours
	 * @param device
	 *            Le "BluetoothDevice" du p�riph�rique connect�
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		// Arr�t du thread "mConnectThread" de mise en connexion
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		// Arr�t de l'�ventuel thread de gestion d'une connexion �tablie
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		// Instanciation du thread de maintien de la connexion
		mConnectedThread = new ConnectedThread(socket);
		// D�marrer ce thread
		mConnectedThread.start();
		// Pr�paration du message qui sera affect� du nom du p�riph�rique
		// Bluetooth
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		// Instanciation et affectation d'un "Bundle" avec ce nom
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		// Renvoyer le nom du p�riph�rique vers l'activit� principale
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		// Nouvel �tat de la connexion transmis � l'UI
		setState(STATE_CONNECTED);
	}

	/**
	 * Methode d'arr�t de tous les threads
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

		setState(STATE_NONE); // Nouvel �tat de connexion
	}

	/**
	 * Method used to write !
	 * 
	 * @param out
	 *            Une chaine de caract�re! string!
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(String out) {
		// Cr�ation d'un objet "ConnectedThread" temporaire
		ConnectedThread r;
		// Synchroniation de la copie du ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			// Uniquement en mode connect�
			r = mConnectedThread; // Instanciation de l'objet temporaire
		}
		// Ecriture effective
		r.sendTestString(out);
	}

	/**
	 * M�thode appel�e quand une connexion n'a pas pu �tre �tablie pour en
	 * notifier l'activit� principale UI
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN); // Nouvel �tat de la connexion
		// Pr�paration et renvoi d'un message de perte vers l'activit�
		// principale
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Bluetooth connection failed !");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * M�thode appel�e quand une connexion est rompue pour en notifier
	 * l'activit� principale UI
	 */
	public void connectionLost() {
		setState(STATE_LISTEN);// Nouvel �tat de la connexion
		// Pr�paration et renvoi d'un message de rupture vers l'activit�
		// principale
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "Bluetooth connection lost");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * Ce thread est actif jusqu'� la connexion effective avec le p�riph�rique
	 * BT ou apr�s un d�lai pr�-d�termin�.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket; // Interface de gestion d'une
												// liaison BT RFCOMM
		private final BluetoothDevice mmDevice; // Objet associ� au p�riph�rique
												// BT externe

		// Constructeur du thread appel� ds la m�thode "connect"
		// L'argument "device" est affect� avec le p�riph�riques BT choisi

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmpSocket = null; // Classe temporaire de gestion
												// des canaux
												// d'�criture/lecture Bluetooth
			// Instanciation de la classe "BluetoothSocket"
			try {// Cr�ation d'un socket RFCOM avec l'UUID donn� en argument
				tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
			}
			// Affectation du socket d�finitif avec le socket temporaire
			mmSocket = tmpSocket;
		}

		// M�thode "run" du "ConnectThread"
		// Appel�e r�guli�rement par le gestionnaire de t�che jusqu'�
		// l'�tablissement
		// de la connexion
		public void run() {
			setName("ConnectThread");
			// Arr�t de la d�couverte car cette fonction est gourmande en
			// �nergie
			if (mAdapter.isDiscovering())
				mAdapter.cancelDiscovery();
			// Etablir une connexion avec le "BluetoothSocket"
			try {
				// Cet appel est bloquant et ne revient qu'apr�s l'�tablissement
				// de la
				// connexion ou en cas d'exception
				mmSocket.connect(); // Connexion effective. Le code PIN sera
									// demand�
			} catch (IOException e) {// En cas d'exceptions : connexion BT
										// impossible
				connectionFailed();
				try {
					mmSocket.close(); // Fermeture du "socket"
				} catch (IOException e2) {
				}
				return;
			}
			// Annulation synchronis�e du ConnectThread
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}
			// Connexion �tablie : d�marrer le "ConnectedThread"
			connected(mmSocket, mmDevice);
		}

		// M�thode de fermeture de ce thread
		public void cancel() {
			try {
				mmSocket.close(); // Fermeture du "Socket"
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Ce thread est actif tant que la connexion avec le p�riph�rique BT est
	 * �tablie Elle g�re les transmissions entrantes et sortantes
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket; // Interface de gestion d'une
												// liaison BT RFCOMM
		@SuppressWarnings("unused")
		private final InputStream mmInStream; // Classe de gestion du flux
												// entrant
		private OutputStream mmOutStream; // Classe de gestion du flux
											// sortant

		// Constructeur du thread appel� ds la m�thode "connected"
		// L'argument "socket" est affect� pour la connexion en cours

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
			mmInStream = tmpIn; // Instanciation des classes d�finitives si pas
								// d'exception
			mmOutStream = tmpOut;
		}

		// M�thode "run" du "ConnectedThread"
		// Appel�e r�guli�rement par le gestionnaire de t�che jusqu'� la rupture
		// de la connexion
		public void run() {
			// Arreter la d�couverte si besoin
			mAdapter.cancelDiscovery();

			try {
				// se connecter sans arr�t � la socket
				mmSocket.connect();
			} catch (IOException e) {
				e.printStackTrace();

			}

			try {
				// envoyer des donn�es
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