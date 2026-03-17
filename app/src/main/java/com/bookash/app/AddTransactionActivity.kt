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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddTransaction"
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val REQUEST_STORAGE_PERMISSION = 1002
        const val EXTRA_TRANSACTION_ID = "transaction_id"
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
    
    // Tags - múltiplas (até 3)
    private lateinit var tagsContainer: LinearLayout
    private lateinit var selectedTagsGroup: com.google.android.material.chip.ChipGroup
    private lateinit var addTagButton: LinearLayout
    private lateinit var addTagText: TextView
    
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
    
    // Modos
    private lateinit var normalModeLayout: LinearLayout
    private lateinit var transferModeLayout: LinearLayout
    
    // Transferência
    private lateinit var transferDateInput: TextInputEditText
    private lateinit var fromAccountField: LinearLayout
    private lateinit var fromAccountIcon: ImageView
    private lateinit var fromAccountText: TextView
    private lateinit var toAccountField: LinearLayout
    private lateinit var toAccountIcon: ImageView
    private lateinit var toAccountText: TextView
    private lateinit var transferObservationInput: TextInputEditText
    
    // Tags transferência - múltiplas (até 3)
    private lateinit var transferTagsContainer: LinearLayout
    private lateinit var transferSelectedTagsGroup: com.google.android.material.chip.ChipGroup
    private lateinit var transferAddTagButton: LinearLayout
    private lateinit var transferAddTagText: TextView
    
    // Dados transferência
    private var fromAccount: Account? = null
    private var toAccount: Account? = null

    // Dados
    private var categories: List<Category> = emptyList()
    private var accounts: List<Account> = emptyList()
    private var tags: List<Tag> = emptyList()

    // Dados selecionados
    private var selectedCategory: Category? = null
    private var selectedAccount: Account? = null
    private var selectedTags: MutableList<Tag> = mutableListOf() // Até 3 tags
    private var transferSelectedTags: MutableList<Tag> = mutableListOf() // Tags para transferência
    private var selectedDate: Date = Date()
    private var selectedReminderDate: Date? = null
    private var selectedImageUri: Uri? = null

    // Tipo de transação
    private var transactionType: String = "income"
    
    // User ID
    private var userId: String? = null
    
    // Modo edição
    private var editingTransactionId: String? = null
    private var editingTransaction: Transaction? = null
    
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

        // Verificar se é modo edição
        editingTransactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)

        initViews()
        setupListeners()
        
        // Título modo edição (após initViews)
        if (editingTransactionId != null) {
            titleText.text = "Editar Transação"
        }
        
        loadData()
        
        // Carregar transação para edição
        if (editingTransactionId != null) {
            loadTransactionForEdit()
        }
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
        
        // Tags - múltiplas
        tagsContainer = findViewById(R.id.tagsContainer)
        selectedTagsGroup = findViewById(R.id.selectedTagsGroup)
        addTagButton = findViewById(R.id.addTagButton)
        addTagText = findViewById(R.id.addTagText)
        
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
        
        // Modos
        normalModeLayout = findViewById(R.id.normalModeLayout)
        transferModeLayout = findViewById(R.id.transferModeLayout)
        
        // Transferência
        transferDateInput = findViewById(R.id.transferDateInput)
        fromAccountField = findViewById(R.id.fromAccountField)
        fromAccountIcon = findViewById(R.id.fromAccountIcon)
        fromAccountText = findViewById(R.id.fromAccountText)
        toAccountField = findViewById(R.id.toAccountField)
        toAccountIcon = findViewById(R.id.toAccountIcon)
        toAccountText = findViewById(R.id.toAccountText)
        transferObservationInput = findViewById(R.id.transferObservationInput)
        
        // Tags transferência - múltiplas
        transferTagsContainer = findViewById(R.id.transferTagsContainer)
        transferSelectedTagsGroup = findViewById(R.id.transferSelectedTagsGroup)
        transferAddTagButton = findViewById(R.id.transferAddTagButton)
        transferAddTagText = findViewById(R.id.transferAddTagText)

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
                updateModeVisibility()
            }
        }

        // Data
        dateInput.setOnClickListener { showDatePicker() }
        dateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showDatePicker() }
        
        // Data transferência
        transferDateInput.setOnClickListener { showTransferDatePicker() }
        transferDateInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) showTransferDatePicker() }

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
        addTagButton.setOnClickListener { showTagPicker() }

        // Frequência - campo clicável
        frequencyDropdown.setOnClickListener { showFrequencyPicker() }
        
        // Transferência - campos clicáveis
        fromAccountField.setOnClickListener { showFromAccountPicker() }
        toAccountField.setOnClickListener { showToAccountPicker() }
        transferAddTagButton.setOnClickListener { showTransferTagPicker() }

        // Salvar
        saveButton.setOnClickListener { saveTransaction() }
    }
    
    private fun updateModeVisibility() {
        if (transactionType == "transfer") {
            normalModeLayout.visibility = View.GONE
            transferModeLayout.visibility = View.VISIBLE
        } else {
            normalModeLayout.visibility = View.VISIBLE
            transferModeLayout.visibility = View.GONE
        }
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
        
        // Filtrar categorias pelo tipo de transação selecionado
        val filteredCategories = when (transactionType) {
            "income" -> categories.filter { it.type == "income" }
            "expense" -> categories.filter { it.type == "expense" }
            "transfer" -> categories.filter { it.type == "transfer" }
            else -> categories
        }
        
        if (filteredCategories.isEmpty()) {
            val typeLabel = when (transactionType) {
                "income" -> "receitas"
                "expense" -> "despesas"
                "transfer" -> "transferências"
                else -> "este tipo"
            }
            ToastManager.showInfo(this, "Nenhuma categoria de $typeLabel cadastrada")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredCategories.map { it.name }))
        popup.anchorView = categoryField
        popup.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = filteredCategories[position]
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
        
        // Filtrar tags que já estão selecionadas
        val availableTags = tags.filter { tag -> 
            selectedTags.none { it.id == tag.id }
        }
        
        if (availableTags.isEmpty()) {
            ToastManager.showInfo(this, "Máximo de 3 tags selecionadas")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, availableTags.map { it.name }))
        popup.anchorView = addTagButton
        popup.setOnItemClickListener { _, _, position, _ ->
            val tag = availableTags[position]
            addSelectedTag(tag)
            popup.dismiss()
        }
        popup.show()
    }
    
    private fun addSelectedTag(tag: Tag) {
        if (selectedTags.size >= 3) {
            ToastManager.showWarning(this, "Máximo de 3 tags permitidas")
            return
        }
        
        selectedTags.add(tag)
        updateSelectedTagsUI()
    }
    
    private fun removeSelectedTag(tag: Tag) {
        selectedTags.remove(tag)
        updateSelectedTagsUI()
    }
    
    private fun updateSelectedTagsUI() {
        if (selectedTags.isEmpty()) {
            selectedTagsGroup.visibility = View.GONE
            addTagText.text = "Adicionar tag"
        } else {
            selectedTagsGroup.visibility = View.VISIBLE
            selectedTagsGroup.removeAllViews()
            
            for (tag in selectedTags) {
                val chip = com.google.android.material.chip.Chip(this).apply {
                    text = tag.name
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        removeSelectedTag(tag)
                    }
                    
                    // Aplicar cor da tag com bom contraste
                    try {
                        val tagColor = Color.parseColor(tag.color)
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(tagColor)
                        
                        // Calcular cor do texto para contraste
                        val textColor = if (isColorDark(tagColor)) {
                            Color.WHITE
                        } else {
                            Color.parseColor("#1A1A1A")
                        }
                        setTextColor(textColor)
                        closeIconTint = android.content.res.ColorStateList.valueOf(textColor)
                    } catch (e: Exception) {
                        // Fallback para cores padrão
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(getColor(R.color.primary))
                        setTextColor(Color.WHITE)
                    }
                }
                selectedTagsGroup.addView(chip)
            }
            
            // Atualizar texto do botão
            val remaining = 3 - selectedTags.size
            addTagText.text = if (remaining > 0) "Adicionar tag ($remaining restantes)" else "Limite atingido"
            addTagButton.isEnabled = selectedTags.size < 3
            addTagButton.alpha = if (selectedTags.size < 3) 1f else 0.5f
        }
    }
    
    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun showTransferDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                selectedDate = cal.time
                updateTransferDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    
    private fun updateTransferDateDisplay() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        transferDateInput.setText(dateFormat.format(selectedDate))
    }
    
    private fun showFromAccountPicker() {
        if (accounts.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma conta cadastrada")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, accounts.map { it.name }))
        popup.anchorView = fromAccountField
        popup.setOnItemClickListener { _, _, position, _ ->
            fromAccount = accounts[position]
            fromAccountText.text = fromAccount?.name
            fromAccountText.setTextColor(getColor(R.color.text_primary))
            fromAccountIcon.visibility = View.VISIBLE
            val iconRes = getBankIconResource(fromAccount?.icon ?: "wallet")
            fromAccountIcon.setImageResource(iconRes)
            popup.dismiss()
        }
        popup.show()
    }
    
    private fun showToAccountPicker() {
        if (accounts.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma conta cadastrada")
            return
        }
        
        // Filtrar para não mostrar a conta de origem
        val availableAccounts = accounts.filter { it.id != fromAccount?.id }
        if (availableAccounts.isEmpty()) {
            ToastManager.showWarning(this, "Selecione uma conta de origem diferente")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, availableAccounts.map { it.name }))
        popup.anchorView = toAccountField
        popup.setOnItemClickListener { _, _, position, _ ->
            toAccount = availableAccounts[position]
            toAccountText.text = toAccount?.name
            toAccountText.setTextColor(getColor(R.color.text_primary))
            toAccountIcon.visibility = View.VISIBLE
            val iconRes = getBankIconResource(toAccount?.icon ?: "wallet")
            toAccountIcon.setImageResource(iconRes)
            popup.dismiss()
        }
        popup.show()
    }
    
    private fun showTransferTagPicker() {
        if (tags.isEmpty()) {
            ToastManager.showWarning(this, "Nenhuma tag cadastrada")
            return
        }
        
        // Filtrar tags que já estão selecionadas
        val availableTags = tags.filter { tag -> 
            transferSelectedTags.none { it.id == tag.id }
        }
        
        if (availableTags.isEmpty()) {
            ToastManager.showInfo(this, "Máximo de 3 tags selecionadas")
            return
        }
        
        val popup = ListPopupWindow(this)
        popup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, availableTags.map { it.name }))
        popup.anchorView = transferAddTagButton
        popup.setOnItemClickListener { _, _, position, _ ->
            val tag = availableTags[position]
            addTransferSelectedTag(tag)
            popup.dismiss()
        }
        popup.show()
    }
    
    private fun addTransferSelectedTag(tag: Tag) {
        if (transferSelectedTags.size >= 3) {
            ToastManager.showWarning(this, "Máximo de 3 tags permitidas")
            return
        }
        
        transferSelectedTags.add(tag)
        updateTransferSelectedTagsUI()
    }
    
    private fun removeTransferSelectedTag(tag: Tag) {
        transferSelectedTags.remove(tag)
        updateTransferSelectedTagsUI()
    }
    
    private fun updateTransferSelectedTagsUI() {
        if (transferSelectedTags.isEmpty()) {
            transferSelectedTagsGroup.visibility = View.GONE
            transferAddTagText.text = "Adicionar tag"
        } else {
            transferSelectedTagsGroup.visibility = View.VISIBLE
            transferSelectedTagsGroup.removeAllViews()
            
            for (tag in transferSelectedTags) {
                val chip = com.google.android.material.chip.Chip(this).apply {
                    text = tag.name
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        removeTransferSelectedTag(tag)
                    }
                    
                    // Aplicar cor da tag com bom contraste
                    try {
                        val tagColor = Color.parseColor(tag.color)
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(tagColor)
                        
                        // Calcular cor do texto para contraste
                        val textColor = if (isColorDark(tagColor)) {
                            Color.WHITE
                        } else {
                            Color.parseColor("#1A1A1A")
                        }
                        setTextColor(textColor)
                        closeIconTint = android.content.res.ColorStateList.valueOf(textColor)
                    } catch (e: Exception) {
                        chipBackgroundColor = android.content.res.ColorStateList.valueOf(getColor(R.color.primary))
                        setTextColor(Color.WHITE)
                    }
                }
                transferSelectedTagsGroup.addView(chip)
            }
            
            // Atualizar texto do botão
            val remaining = 3 - transferSelectedTags.size
            transferAddTagText.text = if (remaining > 0) "Adicionar tag ($remaining restantes)" else "Limite atingido"
            transferAddTagButton.isEnabled = transferSelectedTags.size < 3
            transferAddTagButton.alpha = if (transferSelectedTags.size < 3) 1f else 0.5f
        }
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

    private fun loadTransactionForEdit() {
        saveButton.isEnabled = false
        lifecycleScope.launch {
            // Carregar categorias e contas se ainda não carregadas
            if (categories.isEmpty()) {
                categories = userId?.let { SupabaseService.getCategories(it) } ?: emptyList()
            }
            if (accounts.isEmpty()) {
                accounts = userId?.let { SupabaseService.getAccounts(it) } ?: emptyList()
            }
            Log.d(TAG, "Dados para edição: ${categories.size} categorias, ${accounts.size} contas")
            
            val tx = editingTransactionId?.let { SupabaseService.getTransactionById(it) }
            if (tx != null) {
                editingTransaction = tx
                populateForm(tx)
                saveButton.isEnabled = true
                Log.d(TAG, "Transação carregada para edição: ${tx.id}")
            } else {
                ToastManager.showError(this@AddTransactionActivity, "Transação não encontrada")
                finish()
            }
        }
    }

    private fun populateForm(tx: Transaction) {
        // Tipo
        transactionType = tx.type
        when (tx.type) {
            "income" -> {
                typeToggle.check(R.id.btnIncome)
                titleText.text = "Editar Receita"
            }
            "expense" -> {
                typeToggle.check(R.id.btnExpense)
                titleText.text = "Editar Despesa"
            }
            "transfer" -> {
                typeToggle.check(R.id.btnTransfer)
                titleText.text = "Editar Transferência"
            }
        }
        updateModeVisibility()
        
        // Valor
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        valueInput.setText(formatter.format(tx.amount))
        
        // Descrição
        descriptionInput.setText(tx.description)
        
        // Observações
        notesInput.setText(tx.notes ?: "")
        
        // Data
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(tx.date) ?: Date()
            selectedDate = date
            val displaySdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            dateInput.setText(displaySdf.format(date))
            if (tx.type == "transfer") {
                transferDateInput.setText(displaySdf.format(date))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parsear data", e)
        }
        
        // Status
        receivedSwitch.isChecked = tx.status == "completed"
        
        // Categoria
        selectedCategory = categories.find { it.id == tx.categoryId }
        selectedCategory?.let { cat ->
            categoryText.text = cat.name
            categoryText.setTextColor(getColor(R.color.text_primary))
            categoryIcon.visibility = View.VISIBLE
        }
        
        // Conta (income/expense)
        if (tx.type != "transfer") {
            selectedAccount = accounts.find { it.id == tx.accountId }
            selectedAccount?.let { acc ->
                accountText.text = acc.name
                accountText.setTextColor(getColor(R.color.text_primary))
                accountIcon.visibility = View.VISIBLE
            }
        } else {
            // Transferência
            fromAccount = accounts.find { it.id == tx.fromAccountId }
            fromAccount?.let { acc ->
                fromAccountText.text = acc.name
                fromAccountText.setTextColor(getColor(R.color.text_primary))
                fromAccountIcon.visibility = View.VISIBLE
            }
            toAccount = accounts.find { it.id == tx.toAccountId }
            toAccount?.let { acc ->
                toAccountText.text = acc.name
                toAccountText.setTextColor(getColor(R.color.text_primary))
                toAccountIcon.visibility = View.VISIBLE
            }
            transferObservationInput.setText(tx.description)
        }
        
        // Recorrência
        if (tx.isRecurring) {
            // Primeira transação da série - mostra campos de recorrência
            repeatSwitch.isChecked = true
            repeatFrequencyLayout.visibility = View.VISIBLE
            frequencyText.text = when (tx.recurringType) {
                "daily" -> "Diario"
                "weekly" -> "Semanal"
                "monthly" -> "Mensal"
                "yearly" -> "Anual"
                else -> "Mensal"
            }
            tx.recurringCount?.let { frequencyCountInput.setText(it.toString()) }
        } else if (tx.recurringId != null) {
            // Ocorrência subsequente de uma série - mostrar indicador visual
            // O switch fica desmarcado mas mostramos que faz parte de uma série
            repeatSwitch.isChecked = false
        }
        
        // Tags
        selectedTags.clear()
        selectedTags.addAll(tx.tags)
        updateSelectedTagsUI()
        
        // Expandir "Mais Detalhes" se houver campos preenchidos nessa seção
        if (tx.isRecurring || !tx.notes.isNullOrEmpty()) {
            moreDetailsLayout.visibility = View.VISIBLE
            isMoreDetailsVisible = true
            moreDetailsArrow.rotation = 180f
        }
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

        // Validações diferentes para transferência
        if (transactionType == "transfer") {
            if (fromAccount == null) {
                ToastManager.showWarning(this, "Selecione a conta de origem")
                return
            }
            if (toAccount == null) {
                ToastManager.showWarning(this, "Selecione a conta de destino")
                return
            }
            if (fromAccount?.id == toAccount?.id) {
                ToastManager.showWarning(this, "Contas de origem e destino devem ser diferentes")
                return
            }
        } else {
            val description = descriptionInput.text.toString().trim()
            if (description.isEmpty()) {
                ToastManager.showWarning(this, "Digite uma descrição")
                return
            }
            if (selectedAccount == null) {
                ToastManager.showWarning(this, "Selecione uma conta")
                return
            }
        }

        saveButton.isEnabled = false

        // Verificar se é transação recorrente e mostrar diálogo de escopo
        if (editingTransactionId != null && editingTransaction?.recurringId != null) {
            showEditScopeDialog(value)
            return
        }

        lifecycleScope.launch {
            try {
                performSave(value)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar", e)
                ToastManager.showError(this@AddTransactionActivity, "Erro: ${e.message}")
            } finally {
                saveButton.isEnabled = true
            }
        }
    }

    private fun showEditScopeDialog(value: Double) {
        val options = arrayOf("Apenas esta ocorrência", "Esta e futuras ocorrências")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Editar transação recorrente")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> editSingleTransaction(value)
                    1 -> editThisAndFutureTransactions(value)
                }
            }
            .setOnCancelListener {
                saveButton.isEnabled = true
            }
            .show()
    }

    private fun editSingleTransaction(value: Double) {
        lifecycleScope.launch {
            try {
                performSave(value)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao editar", e)
                ToastManager.showError(this@AddTransactionActivity, "Erro: ${e.message}")
            } finally {
                saveButton.isEnabled = true
            }
        }
    }

    private fun editThisAndFutureTransactions(value: Double) {
        lifecycleScope.launch {
            try {
                val recurringId = editingTransaction?.recurringId ?: return@launch
                val token = UserSession.getAccessToken() ?: return@launch
                val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val baseDateStr = isoDateFormat.format(selectedDate)
                val status = if (receivedSwitch.isChecked) "completed" else "pending"
                
                // Buscar todas as transações da série com data >= data atual
                val allRecurring = withContext(Dispatchers.IO) {
                    SupabaseService.getTransactionsByRecurringId(recurringId)
                }
                
                // Filtrar apenas as futuras (incluindo a atual)
                val currentTransactionDate = editingTransaction?.date ?: baseDateStr
                val futureTransactions = allRecurring.filter { it.date >= currentTransactionDate }
                
                Log.d(TAG, "Editando ${futureTransactions.size} transações futuras da série $recurringId")
                
                var successCount = 0
                for (tx in futureTransactions) {
                    val updatedTx = if (transactionType == "transfer") {
                        tx.copy(
                            description = transferObservationInput.text.toString().trim().ifEmpty { "Transferencia" },
                            amount = value,
                            type = "transfer",
                            fromAccountId = fromAccount?.id,
                            toAccountId = toAccount?.id,
                            fromAccountName = fromAccount?.name,
                            toAccountName = toAccount?.name,
                            status = status
                        )
                    } else {
                        tx.copy(
                            description = descriptionInput.text.toString().trim(),
                            categoryId = selectedCategory?.id ?: "",
                            categoryName = selectedCategory?.name ?: "",
                            amount = value,
                            type = transactionType,
                            accountId = selectedAccount?.id,
                            status = status,
                            notes = notesInput.text.toString().trim().ifEmpty { null }
                        )
                    }
                    
                    val success = withContext(Dispatchers.IO) {
                        SupabaseService.updateTransaction(updatedTx, token)
                    }
                    if (success) successCount++
                }
                
                if (successCount > 0) {
                    ToastManager.showSuccess(this@AddTransactionActivity, "$successCount transações atualizadas!")
                    setResult(RESULT_OK)
                    finish()
                } else {
                    ToastManager.showError(this@AddTransactionActivity, "Erro ao atualizar transações")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao editar série", e)
                ToastManager.showError(this@AddTransactionActivity, "Erro: ${e.message}")
            } finally {
                saveButton.isEnabled = true
            }
        }
    }

    private suspend fun performSave(value: Double) {
        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // Modo edição: atualizar transação existente
        if (editingTransactionId != null) {
            val token = UserSession.getAccessToken() ?: ""
            val baseDateStr = isoDateFormat.format(selectedDate)
            val status = if (receivedSwitch.isChecked) "completed" else "pending"
            val updatedTx = if (transactionType == "transfer") {
                editingTransaction!!.copy(
                    description = transferObservationInput.text.toString().trim().ifEmpty { "Transferencia" },
                    amount = value,
                    type = "transfer",
                    date = baseDateStr,
                    fromAccountId = fromAccount?.id,
                    toAccountId = toAccount?.id,
                    fromAccountName = fromAccount?.name,
                    toAccountName = toAccount?.name,
                    status = status,
                    recurringId = editingTransaction!!.recurringId // Preservar recurringId
                )
            } else {
                editingTransaction!!.copy(
                    description = descriptionInput.text.toString().trim(),
                    categoryId = selectedCategory?.id ?: "",
                    categoryName = selectedCategory?.name ?: "",
                    amount = value,
                    type = transactionType,
                    date = baseDateStr,
                    accountId = selectedAccount?.id,
                    status = status,
                    notes = notesInput.text.toString().trim().ifEmpty { null },
                    recurringId = editingTransaction!!.recurringId // Preservar recurringId
                )
            }
            
            val success = withContext(Dispatchers.IO) {
                SupabaseService.updateTransaction(updatedTx, token)
            }
            
            if (success) {
                // Atualizar tags
                val tagsToSave = if (transactionType == "transfer") transferSelectedTags else selectedTags
                withContext(Dispatchers.IO) {
                    SupabaseService.saveTransactionTags(editingTransactionId!!, tagsToSave.map { it.id })
                }
                ToastManager.showSuccess(this@AddTransactionActivity, "Transação atualizada!")
                setResult(RESULT_OK)
                finish()
            } else {
                ToastManager.showError(this@AddTransactionActivity, "Erro ao atualizar transação")
            }
            return
        }
        
        // Modo criação: criar nova(s) transação(ões)
                val baseDate = selectedDate
                val baseDateStr = isoDateFormat.format(baseDate)
                val baseStatus = if (receivedSwitch.isChecked) "completed" else "pending"
                
                // Verificar se é recorrente
                val isRecurring = repeatSwitch.isChecked
                val frequency = if (isRecurring) frequencyText.text.toString() else null
                val recurringType = when (frequency) {
                    "Diario" -> "daily"
                    "Semanal" -> "weekly"
                    "Mensal" -> "monthly"
                    "Anual" -> "yearly"
                    else -> null
                }
                val count = if (isRecurring) {
                    frequencyCountInput.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
                } else {
                    1
                }
                
                // Lista de transações a serem salvas
                val transactionsToSave = mutableListOf<Transaction>()
                
                // Gerar recurringId único para a série (se recorrente)
                val seriesRecurringId = if (isRecurring) java.util.UUID.randomUUID().toString() else null
                
                for (i in 0 until count) {
                    // Calcular data desta ocorrência
                    val occurrenceDate = if (i == 0) baseDate else {
                        val cal = Calendar.getInstance().apply { time = baseDate }
                        when (recurringType) {
                            "daily" -> cal.add(Calendar.DAY_OF_MONTH, i)
                            "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, i)
                            "monthly" -> cal.add(Calendar.MONTH, i)
                            "yearly" -> cal.add(Calendar.YEAR, i)
                            else -> cal
                        }
                        cal.time
                    }
                    val dateStr = isoDateFormat.format(occurrenceDate)
                    
                    // Apenas a primeira transação tem isRecurring=true e campos de recorrência preenchidos
                    val thisIsRecurring = isRecurring && i == 0
                    val thisRecurringType = if (thisIsRecurring) recurringType else null
                    val thisRecurringCount = if (thisIsRecurring) count else null
                    // Calcular recurringUntil (data da última ocorrência) apenas para a primeira
                    val thisRecurringUntil = if (thisIsRecurring) {
                        val lastCal = Calendar.getInstance().apply { time = baseDate }
                        when (recurringType) {
                            "daily" -> lastCal.add(Calendar.DAY_OF_MONTH, count - 1)
                            "weekly" -> lastCal.add(Calendar.WEEK_OF_YEAR, count - 1)
                            "monthly" -> lastCal.add(Calendar.MONTH, count - 1)
                            "yearly" -> lastCal.add(Calendar.YEAR, count - 1)
                            else -> lastCal
                        }
                        isoDateFormat.format(lastCal.time)
                    } else {
                        null
                    }
                    
                    // Construir transação
                    val transaction = if (transactionType == "transfer") {
                        Transaction(
                            userId = userId ?: "",
                            description = transferObservationInput.text.toString().trim().ifEmpty { "Transferencia" },
                            categoryId = "",
                            categoryName = "Transferencia",
                            amount = value,
                            type = "transfer",
                            date = dateStr,
                            fromAccountId = fromAccount?.id,
                            toAccountId = toAccount?.id,
                            status = baseStatus,
                            isRecurring = thisIsRecurring,
                            recurringType = thisRecurringType,
                            recurringCount = thisRecurringCount,
                            recurringUntil = thisRecurringUntil,
                            recurringId = seriesRecurringId
                        )
                    } else {
                        val description = descriptionInput.text.toString().trim()
                        Transaction(
                            userId = userId ?: "",
                            description = description,
                            categoryId = selectedCategory?.id ?: "",
                            categoryName = selectedCategory?.name ?: "",
                            amount = value,
                            type = transactionType,
                            date = dateStr,
                            accountId = selectedAccount?.id,
                            fromAccountId = if (transactionType == "expense") selectedAccount?.id else null,
                            toAccountId = if (transactionType == "income") selectedAccount?.id else null,
                            status = baseStatus,
                            isRecurring = thisIsRecurring,
                            recurringType = thisRecurringType,
                            recurringCount = thisRecurringCount,
                            recurringUntil = thisRecurringUntil,
                            recurringId = seriesRecurringId
                        )
                    }
                    transactionsToSave.add(transaction)
                }
                
                // Salvar todas as transações
                val token = UserSession.getAccessToken() ?: ""
                var firstTransactionId: String? = null
                
                transactionsToSave.forEachIndexed { index, trans ->
                    val transactionId = withContext(Dispatchers.IO) {
                        SupabaseService.saveTransaction(trans, token)
                    }
                    
                    if (transactionId != null && index == 0) {
                        firstTransactionId = transactionId
                        // Salvar tags apenas para a primeira transação
                        val tagsToSave = if (transactionType == "transfer") transferSelectedTags else selectedTags
                        if (tagsToSave.isNotEmpty()) {
                            val tagIds = tagsToSave.map { it.id }
                            withContext(Dispatchers.IO) {
                                SupabaseService.saveTransactionTags(transactionId, tagIds)
                            }
                        }
                    }
                }
                
                if (firstTransactionId != null) {
                    val msg = if (count > 1) "Transações criadas com sucesso!" else "Transação criada!"
                    ToastManager.showSuccess(this@AddTransactionActivity, msg)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    ToastManager.showError(this@AddTransactionActivity, "Erro ao salvar transação")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar", e)
                ToastManager.showError(this@AddTransactionActivity, "Erro: ${e.message}")
            } finally {
                saveButton.isEnabled = true
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
