package com.cusi.revision

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import org.json.JSONArray
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

data class MCQ(val id:String,val caseId:Int,val question:String,val options:List<String>,val correct:Int,val explanation:String)
data class Attempt(val date:String,val score:Int,val total:Int,val percent:Int,val passed:Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CUSIApp(loadQCM(), getSharedPreferences("cusi", Context.MODE_PRIVATE)) }
    }
    private fun loadQCM():List<MCQ>{
        val text=assets.open("cusi_database.json").bufferedReader().use{it.readText()}
        val arr=JSONObject(text).getJSONArray("qcm")
        return (0 until arr.length()).map{ i ->
            val o=arr.getJSONObject(i); val a=o.getJSONArray("options")
            MCQ(o.optString("id"),o.optInt("case_id"),o.optString("question_source"),
                (0 until a.length()).map{j->a.getJSONObject(j).optString("text")},
                o.optInt("correct_index"),o.optString("explanation_source"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CUSIApp(all:List<MCQ>, prefs:android.content.SharedPreferences){
    var screen by remember{mutableStateOf("home")}
    var loggedIn by remember{mutableStateOf(prefs.getBoolean("logged_in", false))}
    var studentName by remember{mutableStateOf(prefs.getString("student_name","") ?: "")}
    Scaffold(
        topBar={TopAppBar(title={Text("🩺 CUSI – Révision")})},
        bottomBar={NavigationBar{
            NavigationBarItem(screen=="home",{screen="home"},icon={},label={Text("Accueil")})
            NavigationBarItem(screen=="exam",{screen="exam"},icon={},label={Text("Examen")})
            NavigationBarItem(screen=="profile",{screen="profile"},icon={},label={Text("Profil")})
            NavigationBarItem(screen=="account",{screen="account"},icon={},label={Text("Compte")})
            NavigationBarItem(screen=="history",{screen="history"},icon={},label={Text("Historique")})
        }}
    ){pad->Box(Modifier.padding(pad)){
        when(screen){
            "exam"->Exam(all,prefs)
            "profile"->Profile(studentName,{name->{studentName=name;prefs.edit().putString("student_name",name).apply()}},{screen="certificate"})
            "certificate"->Certificate(studentName,prefs)
            "account"->AccountScreen(loggedIn, studentName, prefs, { loggedIn = it })
            "history"->History(prefs)
            else->Home(all.size,{screen="exam"},{screen="profile"})
        }
    }}
}

@Composable
fun Home(count:Int,onExam:()->Unit,onProfile:()->Unit){
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("Je révise mes cas cliniques",style=MaterialTheme.typography.headlineSmall)
        Text("Préparation au Certificat Unique en Soins Infirmiers (CUSI)")
        Text("Banque actuelle : $count QCM.")
        Button(onClick=onExam,Modifier.fillMaxWidth()){Text("🎯 Commencer un examen blanc")}
        OutlinedButton(onClick=onProfile,Modifier.fillMaxWidth()){Text("👤 Mon profil")}
    }
}

@Composable
fun Profile(name:String,onSave:(String)->Unit,onCertificate:()->Unit){
    var value by remember(name){mutableStateOf(name)}
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("👤 Profil étudiant",style=MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value,{value=it},Modifier.fillMaxWidth(),label={Text("Nom complet")})
        Button(enabled=value.isNotBlank(),onClick={onSave(value.trim())},Modifier.fillMaxWidth()){Text("Enregistrer")}
        OutlinedButton(onClick=onCertificate,Modifier.fillMaxWidth()){Text("🏆 Voir mon certificat")}
    }
}

@Composable
fun Exam(all:List<MCQ>,prefs:android.content.SharedPreferences){
    var started by remember{mutableStateOf(false)}
    var count by remember{mutableIntStateOf(minOf(20,all.size))}
    var minutes by remember{mutableIntStateOf(30)}
    if(!started){
        Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Text("🎯 Examen blanc",style=MaterialTheme.typography.headlineSmall)
            Text("Questions")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(10,20,30).filter{it<=all.size}.forEach{
                FilterChip(count==it,{count=it},{Text("$it")})
            }}
            Text("Durée")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(15,30,45,60).forEach{
                FilterChip(minutes==it,{minutes=it},{Text("$it min")})
            }}
            Text("Réussite : 50 % ou plus. Une bonne réponse = 1 point.")
            Button(onClick={started=true},Modifier.fillMaxWidth()){Text("Commencer")}
        }
    }else TimedExam(all,count,minutes,prefs)
}

@Composable
fun TimedExam(all:List<MCQ>,count:Int,minutes:Int,prefs:android.content.SharedPreferences){
    val qs=remember{all.shuffled().take(count)}
    val ans=remember{mutableStateListOf<Int?>().apply{repeat(qs.size){add(null)}}}
    var index by remember{mutableIntStateOf(0)}
    var left by remember{mutableIntStateOf(minutes*60)}
    var finished by remember{mutableStateOf(false)}
    LaunchedEffect(finished){while(!finished&&left>0){delay(1000);left--};if(left==0)finished=true}
    if(finished){ResultScreen(qs,ans,prefs);return}
    val q=qs[index]
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("Question ${index+1}/${qs.size}");Text("⏱ ${left/60}:${(left%60).toString().padStart(2,'0')}")
        }
        LinearProgressIndicator(progress={(index+1).toFloat()/qs.size},Modifier.fillMaxWidth())
        Text("Cas ${q.caseId}",style=MaterialTheme.typography.labelMedium)
        Text(q.question,style=MaterialTheme.typography.titleMedium)
        q.options.forEachIndexed{i,opt->OutlinedButton(onClick={ans[index]=i},Modifier.fillMaxWidth()){
            Text("${('A'.code+i).toChar()}. $opt")
        }}
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(enabled=index>0,onClick={index--},Modifier.weight(1f)){Text("Précédente")}
            Button(enabled=ans[index]!=null,onClick={if(index+1>=qs.size)finished=true else index++},Modifier.weight(1f)){
                Text(if(index+1>=qs.size)"Terminer" else "Suivante")
            }
        }
    }
}

@Composable
fun ResultScreen(qs:List<MCQ>,ans:List<Int?>,prefs:android.content.SharedPreferences){
    val score=qs.indices.count{ans[it]==qs[it].correct}
    val pct=if(qs.isEmpty())0 else (score*100f/qs.size).roundToInt()
    val passed=pct>=50
    var saved by remember{mutableStateOf(false)}
    if(!saved){
        val old=JSONArray(prefs.getString("attempts","[]"))
        val obj=JSONObject().put("date",java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date()))
            .put("score",score).put("total",qs.size).put("percent",pct).put("passed",passed)
        old.put(obj);prefs.edit().putString("attempts",old.toString()).apply();saved=true
    }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("📊 Résultat",style=MaterialTheme.typography.headlineSmall)
        Text("$score / ${qs.size} — $pct %")
        Text(if(passed)"🎉 RÉUSSI" else "❌ À REVOIR")
        if(passed)Text("Tu peux consulter l'attestation de réussite depuis Profil.")
        Text("Bonne réponses : $score")
        Text("Mauvaises réponses : ${qs.size-score}")
    }
}

@Composable
fun History(prefs:android.content.SharedPreferences){
    val arr=JSONArray(prefs.getString("attempts","[]"))
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Text("📈 Historique des examens",style=MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
            items((0 until arr.length()).map{arr.getJSONObject(it)}.reversed()){o->
                Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
                    Text(o.optString("date"))
                    Text("${o.optInt("score")} / ${o.optInt("total")} — ${o.optInt("percent")} %")
                    Text(if(o.optBoolean("passed"))"✅ Réussi" else "❌ À revoir")
                }}
            }
        }
    }
}


@Composable
fun AccountScreen(
    loggedIn:Boolean,
    name:String,
    prefs:android.content.SharedPreferences,
    onLoginChange:(Boolean)->Unit
){
    var email by remember{mutableStateOf(prefs.getString("email","") ?: "")}
    var password by remember{mutableStateOf("")}
    var registerName by remember{mutableStateOf(name)}
    var message by remember{mutableStateOf("")}

    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("☁️ Compte CUSI",style=MaterialTheme.typography.headlineSmall)
        if(!loggedIn){
            Text("Créer un compte ou se connecter")
            OutlinedTextField(registerName,{registerName=it},Modifier.fillMaxWidth(),label={Text("Nom complet")})
            OutlinedTextField(email,{email=it},Modifier.fillMaxWidth(),label={Text("E-mail")})
            O
