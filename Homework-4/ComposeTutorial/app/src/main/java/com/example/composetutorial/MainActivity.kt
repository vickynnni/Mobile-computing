package com.example.composetutorial

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.composetutorial.ui.theme.ComposeTutorialTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            MyAppNavHost(navController = navController)
        }
    }
}

@Composable
fun MyAppNavHost(navController: NavHostController) {
    val context = LocalContext.current

    var username by remember { mutableStateOf(loadUsername(context)) }
    var picURI by remember { mutableStateOf(loadProfilePic(context)) }

    NavHost(
        navController = navController,
        startDestination = "mainScreen"
    ) {
        composable("mainScreen") {
            MainScreen(
                username = username,
                picURI = picURI,
                onNavigateToMessages = {navController.navigate("messagesScreen")},
                onNavigateToSettings = {navController.navigate("settingsScreen")}
            )
        }
        composable("messagesScreen") {
            Conversation(
                onNavigateBack = {navController.popBackStack("mainScreen", false)},
                messages = SampleData.conversationSample
            )
        }
        composable("settingsScreen") {
            SettingsScreen(
                onChangeProfilePic = { uri ->
                    picURI = uri

                    uri?.let {
                        saveProfilePic(context,it)
                    }
                },
                onNameChange = { newUsername ->
                    username = newUsername
                    saveUsername(context, newUsername)
                },
                onNavigateBack = {navController.popBackStack("mainScreen", false)},
            )
        }
    }
}

fun loadUsername(context: Context): String {
    val filename = "username.txt"
    return try {
        context.openFileInput(filename).bufferedReader().useLines { lines ->
            lines.fold("") { some, text ->
                "$some$text"
            }
        }
    } catch (e: FileNotFoundException) {
        "Martina"
    }
}

fun saveUsername(context: Context, name: String) {
    val filename = "username.txt"
    context.openFileOutput(filename, Context.MODE_PRIVATE).use { outputStream ->
        outputStream.write(name.toByteArray())
    }
}

fun loadProfilePic(context: Context): Uri? {
    val file = File(context.cacheDir, "profilePic.jpg")
    return Uri.fromFile(file)
}

fun saveProfilePic(context: Context, uri: Uri) {
    val file = File(context.cacheDir, "profilePic.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
}

@Composable
fun MainScreen(username: String, picURI: Uri?, onNavigateToMessages: () -> Unit, onNavigateToSettings: () -> Unit) {

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            Button(
                onClick = onNavigateToMessages
            ) {
                Row(modifier = Modifier.padding(all = 2.dp)) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New messages",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row(modifier = Modifier.padding(all = 2.dp)) {
                    Image(
                        painter = painterResource(R.drawable.profile_bob),
                        contentDescription = "Contact profile picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }

            Button(
                onClick = onNavigateToSettings
            ) {
                Row(modifier = Modifier.padding(all = 2.dp)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Row(modifier = Modifier.padding(all = 8.dp)) {
                Image(
                    painter = if (picURI != null) rememberAsyncImagePainter(picURI)
                    else painterResource(R.drawable.profile_picture),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.clickable {}) {
                    Text(
                        text = "My Profile",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = username,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onChangeProfilePic: (Uri?) -> Unit, onNameChange: (String) -> Unit, onNavigateBack:() -> Unit ) {
    var newUsername by remember { mutableStateOf("") }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            onChangeProfilePic(uri)
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }
    val context = LocalContext.current
    val tag = "SettingsScreen"
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        Log.d(tag, "Permission isGranted: $isGranted")
        if (isGranted) {
            try {
                val serviceIntent = Intent(context, NotificationService::class.java)
                context.startService(serviceIntent)
                Log.d(tag, "Service started successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error starting service", e)
            }
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = newUsername,
            onValueChange = { newUsername = it },
            label = { Text("Enter new name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text("Select Photo")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onNameChange(newUsername); onNavigateBack() }) {
            Text("Save")
        }
        Button(onClick = { permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) {
            Text("Enable Notifications")
        }
    }
}

data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message) {
    val state = rememberScrollState()
    // Add padding around our message
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.profile_bob),
            contentDescription = "Contact profile picture",
            modifier = Modifier
                // Set image size to 40 dp
                .size(40.dp)
                // Clip image to be shaped as a circle
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)

        )

        // Add a horizontal space between the image and the column
        Spacer(modifier = Modifier.width(8.dp))

        // We keep track if the message is expanded or not in this
        // variable
        var isExpanded by remember { mutableStateOf(false) }
        // surfaceColor will be updated gradually from one color to the other
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        // We toggle the isExpanded variable when we click on this Column
        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                // surfaceColor color will be changing gradually from primary to surface
                color = surfaceColor,
                // animateContentSize will change the Surface size gradually
                modifier = Modifier
                    .animateContentSize()
                    .padding(1.dp)
                    .heightIn(min = 30.dp, max = if (isExpanded) 180.dp else 30.dp) // Set height limits
                    .verticalScroll(state)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    // If the message is expanded, we display all its content
                    // otherwise we only display the first line
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {
    ComposeTutorialTheme {
        Surface {
            MessageCard(
                msg = Message("Lexi", "Take a look at Jetpack Compose, it's great!")
            )
        }
    }
}

@Composable
fun Conversation(onNavigateBack: () -> kotlin.Unit, messages: List<Message>) {
    Scaffold { paddingValues ->
        Column (
            modifier = Modifier.padding(paddingValues)
        ){
            Button(
                onClick = onNavigateBack,
            ) {
                Text("Back")
            }
            LazyColumn {
                items(messages) { message ->
                    MessageCard(message)
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewConversation() {
    ComposeTutorialTheme {
        Conversation(onNavigateBack = {}, SampleData.conversationSample)
    }
}

/**
 * SampleData for Jetpack Compose Tutorial
 */
object SampleData {
    // Sample conversation data
    val conversationSample = listOf(
        Message(
            "Bob",
            "Miau...Miau...Miau..."
        ),
        Message(
            "Bob",
            """List of CATdroid versions:
            |Android KitKat (API 19)
            |Android Lollipop (API 21)
            |Android Marshmallow (API 23)
            |Android Nougat (API 24)
            |Android Oreo (API 26)
            |Android Pie (API 28)
            |Android 10 (API 29)
            |Android 11 (API 30)
            |Android 12 (API 31)""".trim()
        ),
        Message(
            "Bob",
            """I think Catlin is my favorite programming language.
            |It's so much fun!""".trim()
        ),
        Message(
            "Bob",
            "Searching for alternatives to XML layouts...Miau"
        ),
        Message(
            "Bob",
            """Miau, take a look at Jetpack Compose, it's great!
            |It's the Android's modern toolkit for building native UI.
            |It simplifies and accelerates UI development on Android.
            |Less code, powerful tools, and intuitive Kotlin APIs :)""".trim()
        ),
        Message(
            "Bob",
            "It's available from API 21+ :)"
        ),
        Message(
            "Bob",
            "Writing Catlin for UI seems so natural, Compose where have you been all my life?"
        ),
        Message(
            "Bob",
            "CATdroid Studio next version's name is Arctic Cat"
        ),
        Message(
            "Bob",
            "CATdroid Studio Arctic Cat tooling for Compose is top notch ^_^"
        ),
        Message(
            "Bob",
            "I didn't know you can now run the emulator directly from CATdroid Studio"
        ),
        Message(
            "Bob",
            "Compose Previews are great to check quickly how a composable layout looks like"
        ),
        Message(
            "Bob",
            "Previews are also interactive after enabling the experimental setting"
        ),
        Message(
            "Bob",
            "Have you tried writing build.gradle with KTS?"
        ),
    )
}

class NotificationService : Service() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val notificationChannel_id = "notification_channel"

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                sendAccelerometerNotification("Accelerometer:\nX: %.2f\nY: %.2f\nZ: %.2f".format(x, y, z))
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupSensor()
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(sensorListener)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannel_id,
                "Accelerometer Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows accelerometer updates"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendAccelerometerNotification(content: String) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val notification = NotificationCompat.Builder(this, notificationChannel_id)
                .setContentTitle("Accelerometer Data")
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setSmallIcon(R.drawable.icono_notif)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            with(NotificationManagerCompat.from(this)) {
                notify(2, notification)
            }
        }
    }

    override fun onBind(intent: Intent?) = null
}