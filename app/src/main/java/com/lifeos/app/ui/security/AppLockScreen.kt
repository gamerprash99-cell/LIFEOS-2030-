package com.lifeos.app.ui.security

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.AppLockType
import com.lifeos.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class RecoveryStep { NONE, ANSWER_QUESTION, NEW_PIN, NEW_PIN_CONFIRM }

@Composable
fun AppLockScreen(lockType: AppLockType, onUnlocked: () -> Unit) {
    val locator=LocalServiceLocator.current; val scope=rememberCoroutineScope(); val context=LocalContext.current; val activity=context as? FragmentActivity
    var pinInput by remember{mutableStateOf("")};var error by remember{mutableStateOf<String?>(null)};var secureReady by remember{mutableStateOf(activity?.let{locator.appLockManager.canAuthenticate()==BiometricManager.BIOMETRIC_SUCCESS}==true)}
    var recovery by remember{mutableStateOf(RecoveryStep.NONE)};var answer by remember{mutableStateOf("")};var question by remember{mutableStateOf<String?>(null)};var newPin by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")}
    val pulse=rememberInfiniteTransition(label="lock").animateFloat(1f,1.07f,infiniteRepeatable(tween(1000),RepeatMode.Reverse),label="lockpulse")
    val date=remember{LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d",Locale.getDefault()))}

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.White,Color(0xFFF8F0FF),Color(0xFFF1E9FF)))).statusBarsPadding().navigationBarsPadding()) {
        if(lockType==AppLockType.BIOMETRIC && recovery==RecoveryStep.NONE){
            Column(Modifier.fillMaxSize().padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Spacer(Modifier.height(10.dp));Surface(shape=RoundedCornerShape(999.dp),color=Color.White,border=androidx.compose.foundation.BorderStroke(1.dp,LifeOSLavender)){Text("✨ LifeOS Safe Vault  •  🟢",color=LifeOSPrimary,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=16.dp,vertical=9.dp))}
                Spacer(Modifier.height(14.dp));Text("$date  •  Sunny day ☀️",color=LifeOSPrimary,fontWeight=FontWeight.Medium);Text(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),style=MaterialTheme.typography.displayLarge,fontWeight=FontWeight.Bold,color=Color(0xFF182238),modifier=Modifier.padding(top=5.dp))
                Spacer(Modifier.weight(.9f));Box(contentAlignment=Alignment.Center){Box(Modifier.size(300.dp).border(1.dp,LifeOSLavender,CircleShape));Box(Modifier.size(250.dp).border(1.dp,Color(0xFFE9D7FF),CircleShape));Surface(shape=CircleShape,color=LifeOSPrimary,shadowElevation=12.dp,modifier=Modifier.size(150.dp).scale(pulse.value)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(Icons.Filled.Fingerprint,"Unlock",tint=Color.White,modifier=Modifier.size(72.dp))}}};Spacer(Modifier.height(34.dp));Text("LifeOS is locked 🌸",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Your habits, sweet memories & routines are safely tucked away. Tap to unlock.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center,modifier=Modifier.padding(horizontal=26.dp,vertical=8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){InfoCard("💧","TODAY'S HABIT","1/2 completed");InfoCard("⏰","NEXT ALARM","06:00 AM")};Spacer(Modifier.weight(1f));if(secureReady){Button(onClick={activity?.let{locator.appLockManager.authenticate(it,onSuccess=onUnlocked,onError={error=it})}},modifier=Modifier.fillMaxWidth().height(60.dp),shape=RoundedCornerShape(22.dp),colors=ButtonDefaults.buttonColors(containerColor=LifeOSPrimary)){Icon(Icons.Filled.Fingerprint,null);Spacer(Modifier.width(10.dp));Text("Unlock securely",fontWeight=FontWeight.Bold)}}else{Button(onClick={activity?.startActivity(if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply{putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)}}else Intent(Settings.ACTION_SECURITY_SETTINGS))},modifier=Modifier.fillMaxWidth().height(60.dp),shape=RoundedCornerShape(22.dp),colors=ButtonDefaults.buttonColors(containerColor=LifeOSPrimary)){Icon(Icons.Filled.Fingerprint,null);Spacer(Modifier.width(10.dp));Text("Set up secure unlock",fontWeight=FontWeight.Bold)}};error?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center,modifier=Modifier.padding(top=8.dp))};Row(horizontalArrangement=Arrangement.spacedBy(18.dp),modifier=Modifier.padding(top=14.dp)){Text("🔒 Use Passcode",color=LifeOSPrimary,fontWeight=FontWeight.SemiBold);Text("•",color=LifeOSLavender);Text("Emergency",color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("🔐 End-to-end encrypted  •  🔒 On-device private",color=LifeOSPrimary.copy(alpha=.65f),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=18.dp,bottom=10.dp))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                Surface(shape=CircleShape,color=LifeOSVioletSoft,modifier=Modifier.size(86.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(Icons.Filled.Lock,null,tint=LifeOSPrimary,modifier=Modifier.size(42.dp))}}
                Text("LifeOS is locked",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp))
                when {
                    recovery==RecoveryStep.ANSWER_QUESTION->{Text(question.orEmpty(),modifier=Modifier.padding(top=18.dp));OutlinedTextField(answer,{answer=it},label={Text("Your answer")},modifier=Modifier.fillMaxWidth().padding(top=8.dp));error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button(onClick={scope.launch{if(locator.settingsStore.verifyRecoveryAnswer(answer)){error=null;newPin="";recovery=RecoveryStep.NEW_PIN}else error="That doesn't match. Try again."}},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("Verify")};TextButton(onClick={recovery=RecoveryStep.NONE}){Text("Cancel")}}
                    recovery==RecoveryStep.NEW_PIN->{Text("Choose a new PIN",modifier=Modifier.padding(top=18.dp));PinField(newPin,"New PIN"){newPin=it};Button(onClick={if(newPin.length<4)error="PIN must be at least 4 digits." else{error=null;confirm="";recovery=RecoveryStep.NEW_PIN_CONFIRM}},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("Next")}}
                    recovery==RecoveryStep.NEW_PIN_CONFIRM->{Text("Confirm your new PIN",modifier=Modifier.padding(top=18.dp));PinField(confirm,"Re-enter PIN"){confirm=it};Button(onClick={if(confirm!=newPin)error="PINs don't match. Try again." else scope.launch{locator.settingsStore.enablePinLock(newPin,question.orEmpty(),answer);onUnlocked()}},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("Reset PIN and unlock")}}
                    lockType==AppLockType.PIN->{PinField(pinInput,"Enter PIN"){pinInput=it};error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button(onClick={scope.launch{if(locator.settingsStore.verifyPin(pinInput))onUnlocked()else{error="Incorrect PIN.";pinInput=""}}},modifier=Modifier.fillMaxWidth().padding(top=12.dp)){Text("Unlock")};TextButton(onClick={scope.launch{val q=locator.settingsStore.recoveryQuestion.first();if(q.isNullOrBlank())error="No recovery question was set up for this PIN."else{question=q;answer="";error=null;recovery=RecoveryStep.ANSWER_QUESTION}}}){Text("Forgot PIN?")}}
                    else->{error?.let{Text(it,color=MaterialTheme.colorScheme.error)};Button(onClick={activity?.let{locator.appLockManager.authenticate(it,onSuccess=onUnlocked,onError={error=it})}},modifier=Modifier.fillMaxWidth().padding(top=16.dp)){Icon(Icons.Filled.Fingerprint,null);Spacer(Modifier.width(8.dp));Text("Unlock securely")}}
                }
            }
        }
    }
}

@Composable private fun PinField(value:String,label:String,onChange:(String)->Unit){OutlinedTextField(value,onChange,label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth().padding(top=8.dp),singleLine=true)}
@Composable private fun InfoCard(icon:String,title:String,value:String){Surface(shape=RoundedCornerShape(20.dp),color=Color.White,border=androidx.compose.foundation.BorderStroke(1.dp,LifeOSLavender),modifier=Modifier.weight(1f)){Column(Modifier.padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(icon);Text(title,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,fontWeight=FontWeight.Bold)}}}
