package com.lifeos.app.ui.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.ai.*
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.ui.components.LifeOSAIOrb
import com.lifeos.app.ui.components.LifeOSCard
import com.lifeos.app.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AiAssistantViewModel(private val aiRepository: AiRepository) : ViewModel() {
    private val _messages = MutableStateFlow(listOf(ChatMessage("assistant", "Hi! I'm your LifeOS AI Assistant — I run fully offline on your device with complete privacy. Ask me anything about your tasks, habits, spending, diary, or mood! 🌸✨")))
    val messages: StateFlow<List<ChatMessage>> = _messages
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    fun send(text: String) { if(text.isBlank()||_busy.value)return; val updated=_messages.value+ChatMessage("user",text.trim());_messages.value=updated;viewModelScope.launch{_busy.value=true;when(val r=aiRepository.chat(updated)){is AiResult.Success->_messages.value=_messages.value+ChatMessage("assistant",r.text);is AiResult.Error->_messages.value=_messages.value+ChatMessage("assistant","Sorry, I hit an error: ${r.message}")};_busy.value=false} }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiAssistantScreen() {
    val locator=LocalServiceLocator.current
    val vm:AiAssistantViewModel=viewModel(factory=LambdaViewModelFactory{AiAssistantViewModel(locator.aiRepository)})
    val messages by vm.messages.collectAsState(); val busy by vm.busy.collectAsState(); var input by remember{mutableStateOf("")}; val listState=rememberLazyListState()
    LaunchedEffect(messages.size,busy){if(messages.isNotEmpty())listState.animateScrollToItem(messages.lastIndex)}
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFDF8FF),Color(0xFFF6EDFF))))) {
        LazyColumn(state=listState,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=18.dp,bottom=190.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(999.dp),color=Color.White,border=BorderStroke(1.dp,LifeOSLavender)){Text("🌸 AI COMPANION ✨",color=LifeOSPrimary,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=14.dp,vertical=8.dp))};Spacer(Modifier.weight(1f));Surface(shape=CircleShape,color=Color.White,border=BorderStroke(2.dp,LifeOSLavender),modifier=Modifier.size(54.dp)){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("🐰")}}};Row(verticalAlignment=Alignment.CenterVertically){LifeOSAIOrb(size=96.dp);Spacer(Modifier.width(16.dp));Column{Text("LifeOS AI ✨",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Text("Your private local assistant 🌸",style=MaterialTheme.typography.titleMedium,color=Color(0xFF9C67D5));Surface(shape=RoundedCornerShape(999.dp),color=LifeOSVioletSoft){Text("● Offline • 100% on-device & private 🔒",color=LifeOSPrimary,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp))}}}} }
            item { if(messages.size==1&&!busy) LazyRow(horizontalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(end=8.dp)){item{Suggestion("✨ How am I doing?"){input="How am I doing?"}};item{Suggestion("✨ Today's tasks"){input="What are my tasks today?"}};item{Suggestion("💧 Habits"){input="How are my habits going?"}}} }
            itemsIndexed(messages,key={i,_->i}){_,m->val user=m.role=="user";Row(Modifier.fillMaxWidth(),horizontalArrangement=if(user)Arrangement.End else Arrangement.Start){if(!user)LifeOSAIOrb(size=42.dp);if(!user)Spacer(Modifier.width(8.dp));Surface(modifier=Modifier.widthIn(max=340.dp),shape=RoundedCornerShape(24.dp),color=if(user)Color.Transparent else Color.White,tonalElevation=if(user)0.dp else 1.dp){Box(modifier=if(user)Modifier.clip(RoundedCornerShape(24.dp)).background(Brush.horizontalGradient(listOf(LifeOSPrimary,Color(0xFF4F46E5))))else Modifier){Text(m.content,color=if(user)Color.White else MaterialTheme.colorScheme.onSurface,style=MaterialTheme.typography.bodyLarge,modifier=Modifier.padding(horizontal=17.dp,vertical=13.dp))}}}}
            if(busy)item{Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=LifeOSPrimary);Spacer(Modifier.width(8.dp));Text("Thinking locally…",color=MaterialTheme.colorScheme.onSurfaceVariant)}}
            item { LifeOSCard { Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("🌸 DAILY SNAPSHOT • SWEET RHYTHM",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Surface(shape=RoundedCornerShape(999.dp),color=Color(0xFFE8FAF1)){Text("On Track 🌱",color=Color(0xFF159A67),modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))}};HorizontalDivider(color=LifeOSLavender.copy(alpha=.45f));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){Snapshot("💧","2,000 ml","Water goal");Snapshot("₹","₹0 spent","Budget")}} } }
        }
        Surface(modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal=16.dp,bottom=86.dp),shape=RoundedCornerShape(22.dp),color=Color.White,shadowElevation=8.dp,border=BorderStroke(1.dp,LifeOSLavender)){Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(input,{input=it},modifier=Modifier.weight(1f),placeholder={Text("Ask LifeOS AI anything…")},maxLines=3,shape=RoundedCornerShape(18.dp),leadingIcon={Text("✦",color=LifeOSPrimary)});Spacer(Modifier.width(8.dp));FloatingActionButton(onClick={vm.send(input);input=""},containerColor=LifeOSPrimary,contentColor=Color.White,shape=RoundedCornerShape(18.dp),modifier=Modifier.size(58.dp)){Icon(Icons.Filled.Send,"Send")}}}
    }
}

@Composable private fun Suggestion(text:String,onClick:()->Unit)=Surface(onClick=onClick,shape=RoundedCornerShape(999.dp),color=Color.White,border=BorderStroke(1.dp,LifeOSLavender)){Text(text,color=LifeOSPrimary,fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(horizontal=15.dp,vertical=10.dp))}
@Composable private fun Snapshot(icon:String,value:String,label:String)=Column(horizontalAlignment=Alignment.CenterHorizontally){Text(icon);Text(value,color=LifeOSPrimary,fontWeight=FontWeight.Bold);Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
