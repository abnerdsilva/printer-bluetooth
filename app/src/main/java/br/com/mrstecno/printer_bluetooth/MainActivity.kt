package br.com.mrstecno.printer_bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    var mBluetoothAdapter: BluetoothAdapter? = null
    lateinit var mmSocket: BluetoothSocket
    lateinit var mmDevice: BluetoothDevice

    lateinit var mmOutputStream: OutputStream
    lateinit var mmInputStream: InputStream
    lateinit var workerThread: Thread

    lateinit var openButton: Button
    lateinit var sendButton: Button
    lateinit var closeButton: Button

    lateinit var mylabel: TextView
    lateinit var myTextbox: EditText

    var stopWorker: Boolean = false

    companion object {
        lateinit var handler: Handler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openButton = open
        sendButton = send
        closeButton = close

        mylabel = label
        myTextbox = entry

        if(!::mmSocket.isInitialized) {
            closeButton.visibility = View.INVISIBLE
            sendButton.visibility = View.INVISIBLE
        }

        openButton.setOnClickListener {
            closeButton.visibility = View.VISIBLE
            sendButton.visibility = View.VISIBLE

            try {
                findBT()
                openBT()
            } catch (e: IOException){
                e.printStackTrace()
            }
        }

        sendButton.setOnClickListener {
            if(::mmSocket.isInitialized){
                if(!mmSocket.isConnected){
                    Toast.makeText(applicationContext, "Porta está fechada", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                try {
                    var msg = myTextbox.text.toString()
                    msg += "\n\n\n\n "

                    mmOutputStream.write(msg.toByteArray())

                    mylabel.setText("Data sent.")

                } catch (e: IOException){
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(applicationContext, "Porta não iniciada!", Toast.LENGTH_SHORT).show()
            }
        }

        closeButton.setOnClickListener {
            if(::mmSocket.isInitialized) {
                if (!mmSocket.isConnected) {
                    Toast.makeText(applicationContext, "Porta está fechada", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                try {
                    stopWorker = true
                    mmOutputStream.close()
                    mmInputStream.close()
                    mmSocket.close()

                    mylabel.setText("Bluetooth Closed!")

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(applicationContext, "Porta não iniciada!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun findBT(){
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter == null){
                mylabel.setText("No bluetooth adapter available")
                return
            }

            if(!mBluetoothAdapter!!.isEnabled){
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetooth, 1)
            }

            val pairedDevices = mBluetoothAdapter!!.bondedDevices.toSet()
            if(pairedDevices.size > 0){
                for(i in pairedDevices){
                    if(i.name.equals("InnerPrinter")){
                        mmDevice = i
                        break
                    }
                }
            }

            mylabel.setText("Bluetooth device found")

        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun openBT(){
        try {
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid)
            mmSocket.connect()

            mmOutputStream = mmSocket.outputStream
            mmInputStream = mmSocket.inputStream

            //beginListenForData()

            mylabel.setText("Bluetooth Opened")
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    fun beginListenForData(){
        try {
            handler = Handler()

            val delimiter: Byte = 10

            var readBuffer: Byte = 1024.toByte()
            var readeBufferPosition: Int = 0

            workerThread = object: Thread(Runnable{
                while (!Thread.currentThread().isInterrupted && !stopWorker){
                    try {
                        val bytesAvailable = mmInputStream.available()

                        if(bytesAvailable > 0){
                            println("************************************ $bytesAvailable")

                            val packageBytes = bytesAvailable.toString().toByteArray()
                            mmInputStream.read(packageBytes)

                            for(i in 0..bytesAvailable){
                                val b = packageBytes[i]
                                if(b == delimiter){
                                    val encodedBytes = readeBufferPosition.toString().toByteArray()

                                    System.arraycopy(
                                        readBuffer, 0,
                                        encodedBytes, 0,
                                        encodedBytes.size
                                    )

//                                    val data = encodedBytes.binarySearch("US-ASCII".toByte()).toString()
//
//                                    readeBufferPosition = 0
//
//                                    handler.post{
//                                        mylabel.setText(data)
//                                    }
                                }
                            }
                        }
                    } catch (e: IOException){
                        e.printStackTrace()
                    }
                }
            }){}

            workerThread.start()

        } catch (e: IOException){
            e.printStackTrace()
        }
    }
}
