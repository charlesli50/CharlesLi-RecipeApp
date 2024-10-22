package com.example.charlesli_recipeapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.charlesli_recipeapp.ui.theme.CharlesLiRecipeAppTheme

// imports
import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import coil.compose.rememberImagePainter
//import kotlinx.coroutines.flow.collectAsState

//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import retrofit2.http.Path


private const val BASE_URL = "https://api.spoonacular.com/"
private const val APIKEY = "nononononono"

class MainActivity : ComponentActivity() {
    private val apiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .build()
            .create(SpoonacularApiService::class.java)
    }

    private val repository by lazy { RecipeRepository(apiService) }
    private val viewModel by lazy {
        val factory = RecipeViewModelFactory(repository)
        ViewModelProvider(this, factory)[RecipeViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CharlesLiRecipeAppTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    RecipeApp(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
                }
            }
        }
    }
}

interface SpoonacularApiService {
    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String,
        @Query("cuisine") cuisine: String? =null,
        @Query("diet") diet: String? = null,
        @Query("maxCalories") maxCalories: String? = null,
        @Query("number") number: Int = 8,
        @Query("apiKey") apiKey: String = APIKEY,
    ): RecipeResponse

//    @GET("recipes/{id}/information")
//    suspend fun getRecipeInfo(
//        @Path("id") id: Int,
//        @Query("apiKey") apiKey: String = APIKEY,
//    ): RecipeDetailResponse
}

//data class RecipeDetailResponse(
//    val id: Int,
//    val title: String,
//    val image: String,
//    val instructions: String,
//    val ingredients: List<Ingredient>
//)

//data class Ingredient(
//    val id: Int,
//    val name: String,
//    val amount: Double,
//    val unit: String
//)

data class RecipeResponse(
    @Json(name = "offset") val offset: Int?,
    @Json(name = "number") val number: Int?,
    @Json(name = "results") val results: List<Recipe>,
    @Json(name = "totalResults") val totalResults: Int? // If you want to capture total results
)

data class Recipe(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "image") val image: String,
    @Json(name = "imageType") val imageType: String
)

class RecipeRepository(private val apiService: SpoonacularApiService) {
    suspend fun fetchRecipes(query: String, cuisine: String? = null, diet: String? = null, maxCalories: String? = null): RecipeResponse {
        return withContext(Dispatchers.IO) {
            apiService.searchRecipes(query, cuisine, diet, maxCalories)
        }
    }
//    suspend fun getRecipeInfo(recipeId: Int): RecipeDetailResponse {
//        return withContext(Dispatchers.IO) {
//            apiService.getRecipeInfo(recipeId)
//        }
//    }
}

class RecipeViewModelFactory(private val repository: RecipeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {  // Use 'override' here
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            return RecipeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes

//    private val _recipeDetail = MutableStateFlow<RecipeDetailResponse?>(null)
//    val recipeDetail: StateFlow<RecipeDetailResponse?> = _recipeDetail

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(RecipeResponse::class.java)

    fun searchRecipes(query: String, cuisine: String? = null, diet: String? = null, maxCalories: String? = null) {
        viewModelScope.launch {
            try {
                val response = repository.fetchRecipes(query, cuisine, diet, maxCalories)

                val jsonResponse = jsonAdapter.toJson(response)
                Log.d("RecipeViewModel", "JSON Response: $jsonResponse")
                _recipes.value = response.results
            } catch (e: Exception) {
                // Handle exception (could log or show an error message)
                Log.e("RecipeViewModel", "Error fetching recipes: ${e.message}")
            }
        }
    }

//    fun getRecipeInfo(id: Int) {
//        viewModelScope.launch {
//            try {
//                val detailResponse = repository.getRecipeInfo(id)
//                _recipeDetail.value = detailResponse
//            } catch (e: Exception) {
//                // Handle exception
//                Log.e("RecipeViewModel", "Error fetching recipes: ${e.message}")
//            }
//        }
//    }
}



@Composable
fun RecipeApp(modifier: Modifier = Modifier, viewModel: RecipeViewModel) {
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) } // Store selected recipe

    if (selectedRecipe == null) {
        RecipeSearch(viewModel = viewModel) { recipe ->
            selectedRecipe = recipe // Set selected recipe on click
        }
    } else {
        RecipeInfo(recipe = selectedRecipe!!) { // Pass a lambda to reset selected recipe
            selectedRecipe = null
        }
    }
}

@Composable
fun RecipeSearch(modifier: Modifier = Modifier, viewModel: RecipeViewModel, onRecipeClick: (Recipe) -> Unit){
    var query by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("") }
    var diet by remember { mutableStateOf("") }
    var maxCalories by remember { mutableStateOf("") }
    val recipes by viewModel.recipes.collectAsState()
//    var clicked by remember { mutableIntStateOf(0)}

    Column(modifier = Modifier.padding(16.dp)) {
        // Text input for query
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Recipes") },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Text input for cuisine
        TextField(
            value = cuisine,
            onValueChange = { cuisine = it },
            label = { Text("Cuisine") },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Text input for diet
        TextField(
            value = diet,
            onValueChange = { diet = it },
            label = { Text("Diet") },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Text input for max calories
        TextField(
            value = maxCalories,
            onValueChange = { maxCalories = it },
            label = { Text("Max Calories") },
            modifier = Modifier.fillMaxWidth()
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Button to submit the search
        Button(
            onClick = {viewModel.searchRecipes(query, cuisine.takeIf { it.isNotEmpty() }, diet.takeIf { it.isNotEmpty() }, maxCalories.takeIf { it.isNotEmpty() })
                      },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display the recipe results
        LazyColumn {
            if (recipes.isEmpty()) {
                item {
                    Text("No recipes found.")
                }
            } else {
                items(recipes) { recipe ->
                    RecipeItem(recipe = recipe, onClick = { onRecipeClick(recipe) } )
                }
            }
        }
    }
}

@Composable
fun RecipeItem(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.LightGray)
            .padding(16.dp)
            .clickable { onClick() }
    ) {
        Image(
            painter = rememberImagePainter(recipe.image),
            contentDescription = recipe.title,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(recipe.title)
    }
}

@Composable
fun RecipeInfo(recipe: Recipe, modifier: Modifier = Modifier, onBackClick: () -> Unit) {
    Column(modifier = modifier.padding(16.dp)) {
        // Back button
        IconButton(onClick = onBackClick) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = recipe.title, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = rememberImagePainter(recipe.image),
            contentDescription = recipe.title,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Add more details from the recipe if available
        Text(text = "Recipe detail!") // Replace with actual details
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CharlesLiRecipeAppTheme {
//        RecipeApp(viewModel: RecipeViewModel)
    }
}