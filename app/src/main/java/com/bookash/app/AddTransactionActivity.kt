package com.bookash.app

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddTransaction"
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val REQUEST_STORAGE_PERMISSION = 1002
    }

    // Views
    private lateinit var typeToggle: MaterialButtonToggleGroup
    private lateinit var btnIncome: MaterialButton
    private lateinit var btnExpense: MaterialButton
    private lateinit var btnTransfer: MaterialButton
    private lateinit var titleText: TextView
    private lateinit var valueInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categoryField: LinearLayout
    private lateinit var categoryIcon: ImageView
    private lateinit var categoryText: TextView
    private lateinit var accountField: LinearLayout
    private lateinit var accountIcon: ImageView
    private lateinit var accountText: TextView
    private lateinit var attachField: LinearLayout
    private lateinit var attachIcon: ImageView
    private lateinit var attachText: TextView
    private lateinit var attachPreview: ImageView
    private lateinit var attachDropdownIcon: ImageView
    private lateinit var dateInput: TextInputEditText
    private lateinit var tagField: LinearLayout
    private lateinit var tagIcon: ImageView
    private lateinit var tagText: TextView
    private lateinit var receivedSwitch: MaterialSwitch
    private lateinit var repeatSwitch: MaterialSwitch
    private lateinit var repeatFrequencyLayout: LinearLayout
    private lateinit var frequencyCountInput: TextInputEditText
    private lateinit var frequencyDropdown: LinearLayout
    private lateinit var frequencyText: TextView
    private lateinit var reminderSwitch: MaterialSwitch
    private lateinit var reminderDateLayout: TextInputLayout
    private lateinit var reminderDateInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    
    // Mais Detalhes
    private lateinit var moreDetailsToggle: LinearLayout
    private lateinit var moreDetailsLayout: LinearLayout
    private lateinit var moreDetailsArrow: ImageView
    private var isMoreDetailsVisible = false

    // Dados
    private var categories: List<Category> = emptyList()
    private var accounts: List<Account> = emptyList()
    private var tags: List<Tag> = emptyList()

    // Dados selecionados
    private var selectedCategory: Category? = null
    private var selectedAccount: Account? = null
    private var selectedTag: Tag? = null
    private var selectedDate: Date = Date()
    private var selectedReminderDate: Date? = null
    private var selectedImageUri: Uri? = null

    // Tipo de transação
    private var transactionType: String = "income"
    
    // User ID
    private var userId: String? = null
    
    // Camera URI temporária
    private var cameraImageUri: Uri? = null
    
    // Activity result launchers
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            updateAttachField(it)
        }
    }
    
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            updateAttachField(cameraImageUri!!)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        
        // Obter userId do UserSession
        userId = UserSession.getUserId()

        initViews()
        setupListeners()
        loadData()
    }

    private fun initViews() {
        typeToggle = findViewById(R.id.typeToggle)
        btnIncome = findViewById(R.id.btnIncome)
        btnExpense = findViewById(R.id.btnExpense)
        btnTransfer = findViewById(R.id.btnTransfer)
        titleText = findViewById(R.id.titleText)
        valueInput = findViewById(R.id.valueInput)
        descriptionInput = findViewById(R.id.descriptionInput)
        categoryField = findViewById(R.id.categoryField)
        categoryIcon = findViewById(R.id.categoryIcon)
        categoryText = findViewById(R.id.categoryText)
        accountField = findViewById(R.id.accountField)
        accountIcon = findViewById(R.id.accountIcon)
        accountText = findViewById(R.id.accountText)
        attachField = findViewById(R.id.attachField)
        attachIcon = findViewById(R.id.attachIcon)
        attachText = findViewById(R.id.attachText)
        attachPreview = findViewById(R.id.attachPreview)
        attachDropdownIcon = findViewById(R.id.attachDropdownIcon)
        dateInput = findViewById(R.id.dateInput)
        tagField = findViewById(R.id.tagField)
        tagIcon = findViewById(R.id.tagIcon)
        tagText = findViewById(R.id.tagText)
        receivedSwitch = findViewById(R.id.receivedSwitch)
        repeatSwitch = findViewById(R.id.repeatSwitch)
        repeatFrequencyLayout = findViewById(R.id.repeatFrequencyLayout)
        frequencyCountInput = findViewById(R.id.frequencyCountInput)
        frequencyDropdown = findViewById(R.id.frequencyDropdown)
        frequencyText = findViewById(R.id.frequencyText)
        reminderSwitch = findViewById(R.id.reminderSwitch)
        reminderDateLayout = findViewById(R.id.reminderDateLayout)
        reminderDateInput = findViewById(R.id.reminderDateInput)
        notesInput = findViewById(R.id.notesInput)
        saveButton = findViewById(R.id.saveButton)
        moreDetailsToggle = findViewById(R.id.moreDetailsToggle)
        moreDetailsLayout = findViewById(R.id.moreDetailsLayout)
        moreDetailsArrow = findViewById(R.id.moreDetailsArrow)

        // Configurar formatação monetária
        valueInput.addTextChangedListener(CurrencyTextWatcher(valueInput))

        // Frequência padrão
        frequencyText.text = "Mensal"

        updateDateDisplay()
    }

    private fun setupListeners() {
        // Toggle de tipo
        typeToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnIncome -> {
                        transactionType = "income"
                        titleText.text = "Nova Receita"
                        receivedLabel?.text = "Recebido"
                    }
                    R.id.btnExpense -> {
                        transactionType = "expense"
                        titleText.text = "Nova Despesa"
                        receivedLabel?.text = "Pago"
                    }
                    R.id.btnTransfer -> {
                        transactionType = "transfer"
                        titleText.text = "Nova Transferência"
                    }
                }
            }
        }

        // Data
        dateInput.setOnClickListener { showDatePicker() }
        dateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }

        // Lembrete
        reminderDateInput.setOnClickListener { showReminderDatePicker() }
        reminderDateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showReminderDatePicker() }

        // Switch Repetir - mostra/esconde frequência
        repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            repeatFrequencyLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Switch Lembrete - mostra/esconde data do lembrete
        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminderDateLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Categoria - campo clicável
        categoryField.setOnClickListener { showCategoryPicker() }

        // Conta - campo clicável
        accountField.setOnClickListener { showAccountPicker() }

        // Mais Detalhes - toggle
        moreDetailsToggle.setOnClickListener { toggleMoreDetails() }

        // Anexo - campo clicável
        attachField.setOnClickListener { showAttachOptions() }

        // Tag - campo clicável
        tagField.setOnClickListener { showTagPicker() }

        // Frequência - campo clicável
        frequencyDropdown.setOnClickListener { showFrequencyPicker() }

        // Salvar
        saveButton.setOnClickListener { saveTransaction() }
    }

    private fun toggleMoreDetails() {
        isMoreDetailsVisible = !isMoreDetailsVisible
        
        if (isMoreDetailsVisible) {
            moreDetailsLayout.visibility = View.VISIBLE
            moreDetailsArrow.animate().rotation(0f).setDuration(200).start()
            // Animação de slide down
            moreDetailsLayout.alpha = 0f
            moreDetailsLayout.translationY = -50f
            moreDetailsLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()
        } else {
            // Animação de slide up antes de esconder
            moreDetailsArrow.animate().rotation(-90f).setDuration(200).start()
            moreDetailsLayout.animate()
                .alpha(0f)
                .translationY(-50f)
                .setDuration(300)
                .withEndAction { moreDetailsLayout.visibility = View.GONE }
                .start()
        }
    }

    private fun showCategoryPicker() {
        if (categories.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma categoria cadastrada")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categories.map { it.name }))
        popup.anchorView = categoryField
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            categoryText.text = selectedCategory?.name
            categoryText.setTextColor(getColor(R.color.text_primary))
            
            // Mostrar ícone
            categoryIcon.visibility = View.VISIBLE
            val iconRes = getIconResource(selectedCategory?.icon ?: "category")
            categoryIcon.setImageResource(iconRes)
            
            try {
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(Color.parseColor(selectedCategory?.color))
                categoryIcon.background = drawable
                categoryIcon.setColorFilter(Color.WHITE)
            } catch (e: Exception) {
                categoryIcon.setBackgroundColor(getColor(R.color.primary))
            }
            
            popup.dismiss()
        }
        popup.show()
    }

    private fun showAccountPicker() {
        if (accounts.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma conta cadastrada")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, accounts.map { it.name }))
        popup.anchorView = accountField
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedAccount = accounts[position]
            accountText.text = selectedAccount?.name
            accountText.setTextColor(getColor(R.color.text_primary))
            
            // Mostrar ícone
            accountIcon.visibility = View.VISIBLE
            val iconRes = getBankIconResource(selectedAccount?.icon ?: "wallet")
            accountIcon.setImageResource(iconRes)
            
            popup.dismiss()
        }
        popup.show()
    }

    private fun showTagPicker() {
        if (tags.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma tag cadastrada")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, tags.map { it.name }))
        popup.anchorView = tagField
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedTag = tags[position]
            tagText.text = selectedTag?.name
            tagText.setTextColor(getColor(R.color.text_primary))
            
            // Mostrar ícone com cor da tag
            tagIcon.visibility = View.VISIBLE
            try {
                tagIcon.setColorFilter(Color.parseColor(selectedTag?.color))
            } catch (e: Exception) {
                tagIcon.setColorFilter(getColor(R.color.text_secondary))
            }
            
            popup.dismiss()
        }
        popup.show()
    }

    private fun showFrequencyPicker() {
        val frequencies = listOf("Diário", "Semanal", "Mensal", "Anual")
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, frequencies))
        popup.anchorView = frequencyDropdown
        popup.setOnItemClickListener { _, _, position, _ ->
            frequencyText.text = frequencies[position]
            popup.dismiss()
        }
        popup.show()
    }

    private fun showAttachOptions() {
        val options = listOf("Galeria", "Tirar foto")
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, options))
        popup.anchorView = attachField
        popup.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> openGallery()
                1 -> openCamera()
            }
            popup.dismiss()
        }
        popup.show()
    }

    private fun openGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_STORAGE_PERMISSION)
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
                return
            }
        }
        galleryLauncher.launch("image/*")
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }
        
        val photoFile = File.createTempFile(
            "photo_${System.currentTimeMillis()}",
            ".jpg",
            cacheDir
        )
        
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        
        cameraLauncher.launch(cameraImageUri)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    ToastManager.showWarning(this, "Permissão de câmera necessária")
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    ToastManager.showWarning(this, "Permissão de armazenamento necessária")
                }
            }
        }
    }

    private fun updateAttachField(uri: Uri) {
        attachIcon.visibility = View.VISIBLE
        attachText.text = "Foto anexada"
        attachText.setTextColor(getColor(R.color.text_primary))
        attachPreview.visibility = View.VISIBLE
        attachPreview.setImageURI(uri)
        attachDropdownIcon.visibility = View.GONE
    }

    private fun loadData() {
        loadCategories()
        loadAccounts()
        loadTags()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            categories = userId?.let { SupabaseService.getCategories(it) } ?: emptyList()
            Log.d(TAG, "Categorias carregadas: ${categories.size}")
        }
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            accounts = userId?.let { SupabaseService.getAccounts(it) } ?: emptyList()
            Log.d(TAG, "Contas carregadas: ${accounts.size}")
        }
    }

    private fun loadTags() {
        lifecycleScope.launch {
            tags = userId?.let { SupabaseService.getTags(it) } ?: emptyList()
            Log.d(TAG, "Tags carregadas: ${tags.size}")
        }
    }

    private val receivedLabel: TextView?
        get() = findViewById(R.id.receivedLabel)

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.time
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showReminderDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedReminderDate ?: Date()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedReminderDate = calendar.time
                updateReminderDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        dateInput.setText(sdf.format(selectedDate))
    }

    private fun updateReminderDateDisplay() {
        selectedReminderDate?.let {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            reminderDateInput.setText(sdf.format(it))
        }
    }

    private fun saveTransaction() {
        val value = CurrencyTextWatcher.parseValue(valueInput.text.toString())
        
        if (value <= 0) {
            ToastManager.showWarning(this, "Digite um valor maior que zero")
            return
        }

        val description = descriptionInput.text.toString().trim()
        if (description.isEmpty()) {
            ToastManager.showWarning(this, "Digite uma descrição")
            return
        }

        if (selectedAccount == null) {
            ToastManager.showWarning(this, "Selecione uma conta")
            return
        }

        saveButton.isEnabled = false
        saveButton.text = "Salvando..."

        lifecycleScope.launch {
            try {
                val tags = selectedTag?.let { listOf(it.id) } ?: emptyList()
                val frequency = if (repeatSwitch.isChecked) frequencyText.text.toString() else null
                val frequencyCount = if (repeatSwitch.isChecked) {
                    frequencyCountInput.text.toString().toIntOrNull() ?: 1
                } else 1
                val isReceived = receivedSwitch.isChecked

                // Formatar data para ISO 8601
                val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateStr = isoDateFormat.format(selectedDate)
                
                // Formatar data do lembrete se existir
                val reminderDateStr = if (reminderSwitch.isChecked && selectedReminderDate != null) {
                    isoDateFormat.format(selectedReminderDate)
                } else null

                val transaction = Transaction(
                    userId = userId ?: "",
                    description = description,
                    category = selectedCategory?.name ?: "",
                    amount = value,
                    type = transactionType,
                    date = dateStr,
                    status = if (isReceived) "paid" else "pending",
                    accountId = selectedAccount!!.id,
                    tags = tags,
                    reminderDate = reminderDateStr,
                    isRecurring = frequency != null,
                    recurrencePeriod = frequency ?: "",
                    recurrenceCount = frequencyCount
                )

                val token = UserSession.getAccessToken() ?: ""
                
                val success = withContext(Dispatchers.IO) {
                    SupabaseService.saveTransaction(transaction, token)
                }

                if (success) {
                    ToastManager.showSuccess(this@AddTransactionActivity, "Transação salva com sucesso!")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    ToastManager.showError(this@AddTransactionActivity, "Erro ao salvar transação")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar transação", e)
                ToastManager.showError(this@AddTransactionActivity, "Erro: ${e.message}")
            } finally {
                saveButton.isEnabled = true
                saveButton.text = "Salvar Transação"
            }
        }
    }

    private fun getIconResource(iconName: String?): Int {
        return when (iconName) {
            "salary" -> R.drawable.ic_icon_salary
            "freelance" -> R.drawable.ic_icon_freelance
            "investment" -> R.drawable.ic_icon_investment
            "food", "restaurant" -> R.drawable.ic_category_food
            "transport", "car" -> R.drawable.ic_category_transport
            "home", "rent" -> R.drawable.ic_icon_home
            "health" -> R.drawable.ic_category_health
            "education" -> R.drawable.ic_category_education
            "entertainment", "games" -> R.drawable.ic_icon_games
            "shopping" -> R.drawable.ic_icon_shopping
            "utilities", "electricity" -> R.drawable.ic_icon_electricity
            "travel" -> R.drawable.ic_icon_travel
            "pets" -> R.drawable.ic_icon_pet
            "beauty" -> R.drawable.ic_icon_beauty
            "subscriptions" -> R.drawable.ic_icon_subscriptions
            else -> R.drawable.ic_category
        }
    }

    private fun getBankIconResource(bankId: String?): Int {
        return when (bankId) {
            "nubank" -> R.drawable.ic_bank_nubank
            "itau" -> R.drawable.ic_bank_itau
            "bradesco" -> R.drawable.ic_bank_bradesco
            "bb" -> R.drawable.ic_bank_bb
            "caixa" -> R.drawable.ic_bank_caixa
            "santander" -> R.drawable.ic_bank_santander
            "inter" -> R.drawable.ic_bank_inter
            "c6" -> R.drawable.ic_bank_c6
            "original" -> R.drawable.ic_bank_original
            "bmg" -> R.drawable.ic_bank_bmg
            "safra" -> R.drawable.ic_bank_safra
            "btg" -> R.drawable.ic_bank_btg
            "next" -> R.drawable.ic_bank_next
            "digio" -> R.drawable.ic_bank_digio
            "neon" -> R.drawable.ic_bank_neon
            "pagseguro" -> R.drawable.ic_bank_pagseguro
            "mercadopago" -> R.drawable.ic_bank_mercadopago
            "picpay" -> R.drawable.ic_bank_picpay
            "banrisul" -> R.drawable.ic_bank_banrisul
            "votorantim" -> R.drawable.ic_bank_votorantim
            "nordeste" -> R.drawable.ic_bank_nordeste
            "wallet" -> R.drawable.ic_bank_wallet
            else -> R.drawable.ic_bank_wallet
        }
    }
}
