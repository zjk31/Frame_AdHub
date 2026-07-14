package cn.maga.lingdongmanhua.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.maga.lingdongmanhua.ui.theme.AdHubTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = koinViewModel<AuthViewModel>()
) {
    val ui by viewModel.ui.collectAsState()
    val focusManager = LocalFocusManager.current

    // 登录成功后跳转
    LaunchedEffect(ui.success) {
        if (ui.success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // 顶部关闭按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, "关闭", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 标题
        Text(
            text = when (ui.mode) {
                AuthMode.LOGIN -> "登录"
                AuthMode.REGISTER -> "注册"
                AuthMode.FORGOT_PASSWORD -> "找回密码"
            },
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (ui.mode) {
                AuthMode.LOGIN -> "欢迎回来，精彩漫画等你发现"
                AuthMode.REGISTER -> "注册账号，解锁全部功能"
                AuthMode.FORGOT_PASSWORD -> "输入用户名和新密码"
            },
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 表单
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 用户名
            AuthTextField(
                value = ui.username,
                onValueChange = viewModel::onUsernameChange,
                label = "用户名",
                placeholder = "请输入用户名",
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            // 密码
            PasswordTextField(
                value = ui.password,
                onValueChange = viewModel::onPasswordChange,
                label = "密码",
                placeholder = "请输入密码",
                imeAction = if (ui.mode == AuthMode.LOGIN) ImeAction.Done else ImeAction.Next,
                onDone = { viewModel.submit() },
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )

            // 确认密码（注册/找回密码）
            if (ui.mode != AuthMode.LOGIN) {
                PasswordTextField(
                    value = ui.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    label = "确认密码",
                    placeholder = "请再次输入密码",
                    imeAction = ImeAction.Done,
                    onDone = { viewModel.submit() }
                )
            }

            // 错误提示
            if (ui.error != null) {
                Text(
                    text = ui.error!!,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 提交按钮
            Button(
                onClick = viewModel::submit,
                enabled = !ui.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (ui.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when (ui.mode) {
                            AuthMode.LOGIN -> "登录"
                            AuthMode.REGISTER -> "注册"
                            AuthMode.FORGOT_PASSWORD -> "修改密码"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 切换模式
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                when (ui.mode) {
                    AuthMode.LOGIN -> {
                        Text("还没有账号？", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            text = "立即注册",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { viewModel.switchMode(AuthMode.REGISTER) }
                        )
                    }
                    AuthMode.REGISTER -> {
                        Text("已有账号？", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            text = "去登录",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { viewModel.switchMode(AuthMode.LOGIN) }
                        )
                    }
                    AuthMode.FORGOT_PASSWORD -> {
                        Text("想起密码了？", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            text = "去登录",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { viewModel.switchMode(AuthMode.LOGIN) }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when (ui.mode) {
                        AuthMode.LOGIN -> "忘记密码？"
                        else -> "忘记用户名？"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        viewModel.switchMode(AuthMode.FORGOT_PASSWORD)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    imeAction: ImeAction,
    onNext: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color.Gray) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
    onNext: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color.Gray) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
            onDone = { onDone() }
        ),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "隐藏密码" else "显示密码",
                    tint = Color.Gray
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(10.dp)
    )
}
