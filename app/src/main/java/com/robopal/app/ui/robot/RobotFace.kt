package com.robopal.app.ui.robot

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.robopal.app.agent.AgentState
import com.robopal.app.ui.theme.DarkBackground
import com.robopal.app.ui.theme.RobotError
import com.robopal.app.ui.theme.RobotGlow
import com.robopal.app.ui.theme.RobotPrimary
import com.robopal.app.ui.theme.RobotSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RobotFace(
    agentState: AgentState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "robot_anim")

    // Animación de respiración
    val breathScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Animación de parpadeo (escala Y de ojos)
    val blinkScaleY by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    // Animación de orbita/rotación (THINKING)
    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // Animación de boca (SPEAKING)
    val mouthHeightScale by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth"
    )

    // Animación de mirada de lado a lado (WORKING)
    val lookOffsetX by transition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "look"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = (size.minDimension / 2f) * 0.70f * (if (agentState == AgentState.IDLE) breathScale else 1.0f)

        // 1. Dibujar esfera central con radial gradient y brillo superior
        val sphereBrush = Brush.radialGradient(
            colors = listOf(RobotPrimary, RobotSecondary, DarkBackground),
            center = Offset(centerX, centerY - baseRadius * 0.2f),
            radius = baseRadius * 1.2f
        )
        drawCircle(
            brush = sphereBrush,
            radius = baseRadius,
            center = Offset(centerX, centerY)
        )

        // Brillo superior de la esfera
        drawCircle(
            color = RobotGlow.copy(alpha = 0.35f),
            radius = baseRadius * 0.35f,
            center = Offset(centerX - baseRadius * 0.25f, centerY - baseRadius * 0.35f)
        )

        // Configuración de ojos
        val eyeDistance = baseRadius * 0.35f
        val eyeBaseRadius = baseRadius * 0.12f

        val leftEyeCenter = Offset(centerX - eyeDistance, centerY - baseRadius * 0.1f)
        val rightEyeCenter = Offset(centerX + eyeDistance, centerY - baseRadius * 0.1f)

        val eyeColor = if (agentState == AgentState.ERROR) RobotError else Color.White

        when (agentState) {
            AgentState.IDLE -> {
                val eyeHeight = eyeBaseRadius * blinkScaleY
                drawOval(
                    color = eyeColor,
                    topLeft = Offset(leftEyeCenter.x - eyeBaseRadius, leftEyeCenter.y - eyeHeight),
                    size = Size(eyeBaseRadius * 2, eyeHeight * 2)
                )
                drawOval(
                    color = eyeColor,
                    topLeft = Offset(rightEyeCenter.x - eyeBaseRadius, rightEyeCenter.y - eyeHeight),
                    size = Size(eyeBaseRadius * 2, eyeHeight * 2)
                )
            }
            AgentState.LISTENING -> {
                val enlargedRadius = eyeBaseRadius * 1.15f
                drawCircle(color = eyeColor, radius = enlargedRadius, center = leftEyeCenter)
                drawCircle(color = eyeColor, radius = enlargedRadius, center = rightEyeCenter)
            }
            AgentState.THINKING -> {
                val orbitRadius = eyeBaseRadius * 0.6f
                val leftOrbit = Offset(
                    leftEyeCenter.x + orbitRadius * cos(orbitAngle),
                    leftEyeCenter.y + orbitRadius * sin(orbitAngle)
                )
                val rightOrbit = Offset(
                    rightEyeCenter.x + orbitRadius * cos(orbitAngle),
                    rightEyeCenter.y + orbitRadius * sin(orbitAngle)
                )
                drawCircle(color = eyeColor, radius = eyeBaseRadius, center = leftOrbit)
                drawCircle(color = eyeColor, radius = eyeBaseRadius, center = rightOrbit)
            }
            AgentState.SPEAKING -> {
                drawCircle(color = eyeColor, radius = eyeBaseRadius, center = leftEyeCenter)
                drawCircle(color = eyeColor, radius = eyeBaseRadius, center = rightEyeCenter)

                // Dibujar boca que oscila
                val mouthWidth = baseRadius * 0.3f
                val mouthMaxHeight = baseRadius * 0.15f
                val mouthHeight = mouthMaxHeight * mouthHeightScale
                val mouthCenterY = centerY + baseRadius * 0.3f

                drawOval(
                    color = eyeColor,
                    topLeft = Offset(centerX - mouthWidth / 2f, mouthCenterY - mouthHeight / 2f),
                    size = Size(mouthWidth, mouthHeight)
                )
            }
            AgentState.ERROR -> {
                // Ojos en forma de X roja
                val crossSize = eyeBaseRadius * 1.2f
                val strokeWidth = 8f

                // X izquierda
                drawLine(
                    color = eyeColor,
                    start = Offset(leftEyeCenter.x - crossSize, leftEyeCenter.y - crossSize),
                    end = Offset(leftEyeCenter.x + crossSize, leftEyeCenter.y + crossSize),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = eyeColor,
                    start = Offset(leftEyeCenter.x - crossSize, leftEyeCenter.y + crossSize),
                    end = Offset(leftEyeCenter.x + crossSize, leftEyeCenter.y - crossSize),
                    strokeWidth = strokeWidth
                )

                // X derecha
                drawLine(
                    color = eyeColor,
                    start = Offset(rightEyeCenter.x - crossSize, rightEyeCenter.y - crossSize),
                    end = Offset(rightEyeCenter.x + crossSize, rightEyeCenter.y + crossSize),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = eyeColor,
                    start = Offset(rightEyeCenter.x - crossSize, rightEyeCenter.y + crossSize),
                    end = Offset(rightEyeCenter.x + crossSize, rightEyeCenter.y - crossSize),
                    strokeWidth = strokeWidth
                )
            }
            AgentState.WORKING -> {
                val leftShifted = Offset(leftEyeCenter.x + lookOffsetX, leftEyeCenter.y)
                val rightShifted = Offset(rightEyeCenter.x + lookOffsetX, rightEyeCenter.y)
                drawCircle(color = eyeColor, radius = eyeBaseRadius, center = leftShifted)
                drawCircle(color = eyeColor, radius = eyeBaseRadius, center = rightShifted)
            }
        }
    }
}
