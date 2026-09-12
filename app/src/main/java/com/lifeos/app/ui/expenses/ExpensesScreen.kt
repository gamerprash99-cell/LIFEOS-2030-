package com.lifeos.app.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifeos.app.core.di.LambdaViewModelFactory
import com.lifeos.app.core.di.LocalServiceLocator
import com.lifeos.app.core.util.DateTimeUtils
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.repository.ExpenseRepository
import com.lifeos.app.domain.model.ExpenseCategories
import com.lifeos.app.ui.components.*
import com.lifeos.app.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpensesViewModel(private val expenseRepository: ExpenseRepository) : ViewModel() {
    private val monthStart=DateTimeUtils.startOfMonthEpochDay();private val monthEnd=DateTimeUtils.endOfMonthEpochDay()
    val expensesThisMonth:StateFlow<List<ExpenseEntity>> = expenseRepository.observeInRange(monthStart,monthEnd).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val totalThisMonth:StateFlow<Double> = expenseRepository.observeTotalInRange(monthStart,monthEnd).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),0.0)
    fun addExpense(amount:Double,category:String,merchant:String?){if(amount<=0.0)return;viewModelScope.launch{val now=DateTimeUtils.today();val minutes=java.time.LocalTime.now().let{it.hour*60+it.minute};expenseRepository.addExpense(amount,category,now.toEpochDay(),minutes,merchant)}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(onBack:()->Unit={}){
    val locator=LocalServiceLocator.current;val vm:ExpensesViewModel=viewModel(factory=LambdaViewModelFactory{ExpensesViewModel(locator.expenseRepository)});val expenses by vm.expensesThisMonth.collectAsState();val total by vm.totalThisMonth.collectAsState();val budget=15000.0;val remaining=(budget-total).coerceAtLeast(0.0);var showAdd by remember{mutableStateOf(false)};var amount by remember{mutableStateOf("")};var merchant by remember{mutableStateOf("")};var category by remember{mutableStateOf(ExpenseCategories.ALL.first().name)}
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)){
        LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp,18.dp,20.dp,112.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
            item{Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(999.dp),color=Color(0xFFFFE9F2),border=BorderStroke(1.dp,Color(0xFFFFC9DE))){Text("🌸 MONEY & BUDGET",color=Color(0xFFC73572),fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=13.dp,vertical=8.dp))};Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(16.dp),color=Color.White,border=BorderStroke(2.dp,LifeOSLavender),modifier=Modifier.size(54.dp)){Box(contentAlignment=Alignment.Center){Text("🐰")}}};Row(verticalAlignment=Alignment.CenterVertically){Text("Expenses ✨",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));IconButton(onClick=onBack){Icon(Icons.Filled.Close,"Back")}}}}
            item{LifeOSCard{Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){Row(verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(16.dp),color=LifeOSVioletSoft){Text("THIS MONTH",color=LifeOSPrimary,fontWeight=FontWeight.Bold,modifier=Modifier.padding(horizontal=13.dp,vertical=8.dp))};Spacer(Modifier.width(10.dp));Text(LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale.getDefault())),color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(999.dp),color=Color(0xFFE8FAF1)){Text("Safe Zone 🌿",color=Color(0xFF159A67),modifier=Modifier.padding(horizontal=11.dp,vertical=7.dp),fontWeight=FontWeight.SemiBold)}};Row(verticalAlignment=Alignment.Bottom){Text("₹${"%.0f".format(total)}",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Text(" / ₹${"%.0f".format(budget)}",style=MaterialTheme.typography.titleMedium,color=LifeOSPrimary,modifier=Modifier.padding(bottom=6.dp))};Text(if(total==0.0)"No expenses logged yet ✨ You're 100% on budget!" else "₹${"%.0f".format(remaining)} left in your monthly target",color=MaterialTheme.colorScheme.onSurfaceVariant);Row(verticalAlignment=Alignment.CenterVertically){Text("Monthly Target",fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));Text("${((total/budget)*100).toInt()}% used",color=LifeOSPrimary,fontWeight=FontWeight.Bold)};LinearProgressIndicator(progress={(total/budget).toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(8.dp),trackColor=LifeOSLavenderSoft,color=LifeOSPrimary);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("DAILY AVG","₹${"%.0f".format(total/LocalDate.now().dayOfMonth)}");Metric("BUDGET LEFT","₹${"%.0f".format(remaining)}");Metric("SAVED","${(100-(total/budget*100)).coerceAtLeast(0.0).toInt()}% 🎯")}}}}
            item{Row(verticalAlignment=Alignment.CenterVertically){Text("Categories 🍰",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("Customize",color=LifeOSPrimary,fontWeight=FontWeight.SemiBold)}}
            item{LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(end=10.dp)){items(ExpenseCategories.ALL.take(5)){cat->Surface(shape=RoundedCornerShape(20.dp),color=Color.White,border=BorderStroke(1.dp,LifeOSLavender),modifier=Modifier.widthIn(min=120.dp)){Column(Modifier.padding(horizontal=14.dp,vertical=11.dp)){Text("${cat.emoji} ${cat.name}",fontWeight=FontWeight.SemiBold);Text("₹0",color=Color(0xFFCA4775),style=MaterialTheme.typography.labelMedium)}}}}}
            item{Row(verticalAlignment=Alignment.CenterVertically){Text("Recent Transactions",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(999.dp),color=LifeOSVioletSoft){Text("This Week",color=LifeOSPrimary,modifier=Modifier.padding(horizontal=13.dp,vertical=8.dp))}}}
            if(expenses.isEmpty())item{LifeOSCard{Column(Modifier.fillMaxWidth().padding(30.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){Text("🐷",style=MaterialTheme.typography.displaySmall);Text("Fresh start! ✨",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Tap + below to log your first coffee, sweet treat, or ride.",color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center);Text("QUICK LOG IDEAS",color=LifeOSPrimary,fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){QuickIdea("☕ Coffee ₹120");QuickIdea("🥑 Grocery ₹450")};QuickIdea("💵 Quick ₹50")}}}
            items(expenses,key={it.id}){expense->LifeOSCard{Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text(ExpenseCategories.emojiFor(expense.category),style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(expense.merchant?:expense.category,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold);Text(expense.category,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text("₹${"%.0f".format(expense.amount)}",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}}}
        }
        FloatingActionButton(onClick={showAdd=true},containerColor=LifeOSPrimary,contentColor=Color.White,shape=CircleShape,modifier=Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end=22.dp,bottom=18.dp)){Icon(Icons.Filled.Add,"Add expense",modifier=Modifier.size(30.dp))}
    }
    if(showAdd)ModalBottomSheet(onDismissRequest={showAdd=false},shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp),containerColor=MaterialTheme.colorScheme.surface){Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=24.dp).padding(bottom=18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("Add expense ✨",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Surface(shape=RoundedCornerShape(999.dp),color=LifeOSVioletSoft){Text("🌸 LifeOS",color=LifeOSPrimary,modifier=Modifier.padding(horizontal=12.dp,vertical=7.dp))}};Text("ENTER AMOUNT *",color=LifeOSPrimary,fontWeight=FontWeight.Bold);OutlinedTextField(amount,{if(it.length<=10)amount=it},modifier=Modifier.fillMaxWidth(),leadingIcon={Text("₹",style=MaterialTheme.typography.headlineSmall,color=LifeOSPrimary)},placeholder={Text("250")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),shape=RoundedCornerShape(20.dp));LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(listOf(50,100,500,1000)){v->AssistChip(onClick={amount=((amount.toDoubleOrNull()?:0.0)+v).toInt().toString()},label={Text("+₹$v")})}};Row(verticalAlignment=Alignment.CenterVertically){Text("MERCHANT / NOTE",fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("optional",color=MaterialTheme.colorScheme.onSurfaceVariant)};OutlinedTextField(merchant,{merchant=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Blue Tokai Coffee")},singleLine=true,leadingIcon={Icon(Icons.Filled.ShoppingBag,null,tint=LifeOSSecondary)},shape=RoundedCornerShape(20.dp));Row(verticalAlignment=Alignment.CenterVertically){Text("CATEGORY",fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Surface(shape=RoundedCornerShape(999.dp),color=LifeOSVioletSoft){Text("● Selected: $category ${ExpenseCategories.emojiFor(category)}",color=LifeOSPrimary,modifier=Modifier.padding(horizontal=11.dp,vertical=7.dp))}};LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(ExpenseCategories.ALL.take(5)){cat->FilterChip(selected=category==cat.name,onClick={category=cat.name},label={Text("${cat.emoji} ${cat.name}")})}};Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Surface(shape=RoundedCornerShape(16.dp),color=LifeOSVioletSoft,modifier=Modifier.weight(1f)){Text("📅 Today, ${LocalDate.now().dayOfMonth}",modifier=Modifier.padding(14.dp))};Surface(shape=RoundedCornerShape(16.dp),color=LifeOSVioletSoft,modifier=Modifier.weight(1f)){Text("💳 UPI / Card",modifier=Modifier.padding(14.dp))}};Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onClick={showAdd=false},modifier=Modifier.weight(1f).height(56.dp),shape=RoundedCornerShape(20.dp)){Text("Cancel")};Button(enabled=(amount.toDoubleOrNull()?:0.0)>0,onClick={vm.addExpense(amount.toDoubleOrNull()?:0.0,category,merchant.ifBlank{null});amount="";merchant="";showAdd=false},modifier=Modifier.weight(1.5f).height(56.dp),shape=RoundedCornerShape(20.dp)){Text("Save Expense ✨",fontWeight=FontWeight.Bold)}}}}
}

@Composable private fun RowScope.Metric(label:String,value:String){Surface(shape=RoundedCornerShape(16.dp),color=Color.White,border=BorderStroke(1.dp,LifeOSLavender),modifier=Modifier.weight(1f)){Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(value,color=LifeOSPrimary,fontWeight=FontWeight.Bold)}}}
@Composable private fun QuickIdea(text:String){Surface(shape=RoundedCornerShape(999.dp),color=Color.White,border=BorderStroke(1.dp,LifeOSLavender)){Text(text,modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp),style=MaterialTheme.typography.labelMedium)}}
