#!/bin/bash
sed -i 's/SmallTopAppBar/TopAppBar/g' app/src/main/java/com/example/ui/AppUI.kt
sed -i 's/Icons.AutoMirrored.Filled.ArrowBack/Icons.Default.ArrowBack/g' app/src/main/java/com/example/ui/AppUI.kt
sed -i 's/TopAppBarDefaults.smallTopAppBarColors/TopAppBarDefaults.topAppBarColors/g' app/src/main/java/com/example/ui/AppUI.kt
