package com.karldenby.diceroller

import com.karldenby.diceroller.model.Dice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import io.realm.*

import kotlin.random.Random

import io.realm.SyncUser.logInAsync
import io.realm.log.LogLevel
import io.realm.log.RealmLog

class MainActivity : AppCompatActivity() {

    // assign after inflation
    private lateinit var diceImage: ImageView
    private lateinit var rollButton: Button

    // realm
    private lateinit var realmConfiguration: RealmConfiguration
    private lateinit var realm: Realm
    private lateinit var realmListener: RealmChangeListener<Realm>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup Realm
        Realm.init(this)
        RealmLog.setLevel(LogLevel.DEBUG)

        val credentials = SyncCredentials.usernamePassword(Constants.REALM_USER, Constants.REALM_PASSWORD)
        logInAsync(credentials, Constants.AUTH_URL, object : SyncUser.Callback<SyncUser> {
                override fun onError(error: ObjectServerError) {
                    Log.e("REALM", "User error $error")
                }
                override fun onSuccess(result: SyncUser) {
                    Log.i("REALM","We have a user")
                }
            }
        )

        val url = Constants.REALM_URL + "/~/dice-roller"
        val data = Dice()
        data.number = 6

        realmConfiguration = SyncUser.current().createConfiguration(url)
            .fullSynchronization()
            .schemaVersion(2)
            .initialData { realm ->
                realm.copyToRealmOrUpdate(data)
                Log.i("REALM", "Inserted default data on $url")
            }
            .build()
        realm = Realm.getInstance(realmConfiguration)

        // now layout has been inflated
        diceImage = findViewById(R.id.dice_image)
        rollButton= findViewById(R.id.roll_button)
        rollButton.setOnClickListener {
            rollDice()
        }
    }

    private fun rollDice(result: Int = Random.nextInt(6) + 1) {

        val cloud = 1
        val data = Dice()
        data.number = result

        realm.removeAllChangeListeners()
        Log.i("REALM", "Roll Dice")
        realm.beginTransaction()
            realm.where(Dice :: class.java).equalTo("cloud", cloud)
            realm.copyToRealmOrUpdate(data)
        realm.commitTransaction()

        val drawableResource = when (result) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            else -> R.drawable.dice_6
        }

        diceImage.setImageResource(drawableResource)

        realmListener = RealmChangeListener<Realm> {

            Log.i("REALM", "Data was updated")

            val query = it.where(Dice :: class.java)
            query.equalTo("cloud", 1 as Int)

            val result = query.findAll()

            Log.i("REALM", "" + result[0]?.number)
            rollDice(result[0]?.number!!)
        }
        realm.addChangeListener(realmListener)
    }

    override fun onPause() {
        super.onPause()
        realm.removeAllChangeListeners()
        realm.close()
        Log.i("REALM","App Paused")
    }

    override fun onStart() {
        super.onStart()
        //realm = Realm.getInstance(realmConfiguration)
        Log.i("REALM","App Paused")
    }
}
