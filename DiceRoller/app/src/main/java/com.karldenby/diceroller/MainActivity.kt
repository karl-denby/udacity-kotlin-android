package com.karldenby.diceroller

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.karldenby.diceroller.Constants.REALM_PASSWORD
import com.karldenby.diceroller.Constants.REALM_URL
import com.karldenby.diceroller.Constants.REALM_USER
import com.karldenby.diceroller.model.Dice
import io.realm.*
import io.realm.SyncUser.logInAsync
import io.realm.log.LogLevel
import io.realm.log.RealmLog
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // UI Elements that we need assign after "inflation"
    private lateinit var diceImage: ImageView
    private lateinit var rollButton: Button

    // Realm things that we will need during different stages of the lifecycle
    private lateinit var realmConfiguration: RealmConfiguration
    private lateinit var realm: Realm
    private lateinit var realmListener: RealmChangeListener<Realm>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup all those lateinit things you promised you would initalize before you use them

        // Setup Realm
        Realm.init(this)
        RealmLog.setLevel(LogLevel.DEBUG)

        // Log in a Realm User or throw an error
        val credentials = SyncCredentials.usernamePassword(REALM_USER, REALM_PASSWORD)
        logInAsync(credentials, Constants.AUTH_URL, object : SyncUser.Callback<SyncUser> {
                override fun onError(error: ObjectServerError) {
                    Log.e("REALM", "User error $error")
                }
                override fun onSuccess(result: SyncUser) {
                    Log.i("REALM","We have a user")
                }
            }
        )

        val url = "${REALM_URL}/~/dice-roller"  // path to our realm file
        val data = Dice()  // we need a variable to store an instance of our dice class which is a RealmObject
        data.number = 6  // set the value of this instance to 6

        // create a configuration to connect to our path
        realmConfiguration = SyncUser.current().createConfiguration(url)
            .fullSynchronization()
            .schemaVersion(2)
            .initialData { realm ->
                realm.copyToRealmOrUpdate(data)
                Log.i("REALM", "Inserted default data on $url")
            }
            .build()

        // connect using our configuration
        realm = Realm.getInstance(realmConfiguration)

        // now UI layout has been inflated we can associate variables with UI elements
        //  also associate our rollDice code with the click event of the roll button
        diceImage = findViewById(R.id.dice_image)
        rollButton= findViewById(R.id.roll_button)
        rollButton.setOnClickListener {
            rollDice()
        }

        // create a changeListener so we get notified if data on the backed changes
        realmListener = RealmChangeListener<Realm> {

            Log.i("REALM", "changeListener active")

            val query = it.where(Dice :: class.java)
            query.equalTo("cloud", 1 as Int)

            val result = query.findAll()

            Log.i("REALM", "" + result[0]?.number)
            rollDice(result[0]?.number!!, false)
        }

        // attach the changeListener to our connected/configured/logged-in realm
        realm.addChangeListener(realmListener)

    }

    private fun rollDice(result: Int = Random.nextInt(6) + 1, write: Boolean = true) {
        // if we are provided a number in result we will use it, if not its a random one
        // we assume we need to write this to the Realm, but give the option to set it false
        //  to prevent race conditions if we get an external update and also try to write the same
        val cloud = 1
        val data = Dice()
        data.number = result

        Log.i("REALM", "Roll Dice")

        // update the ui based on result
        val drawableResource = when (result) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            else -> R.drawable.dice_6
        }
        diceImage.setImageResource(drawableResource)

        // We need to write the result to the realm
        if (write) {
            realm.beginTransaction()
            realm.where(Dice::class.java).equalTo("cloud", cloud)
            realm.copyToRealmOrUpdate(data)
            realm.commitTransaction()
        }

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

        val query = realm.where(Dice :: class.java)
        query.equalTo("cloud", 1 as Int)
        val result = query.findAll()
        Log.i("REALM", "" + result[0]?.number)
        rollDice(result[0]?.number!!, false)

        Log.i("REALM","App Paused")
    }

}
