package com.zelretch.aniiiiict.testing

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * HiltとCompose UIテストのセットアップを簡略化するカスタムTestRule.
 *
 * このルールは以下の定型的な処理をまとめます:
 * - `HiltAndroidRule` の適用
 * - `ComposeTestRule` の内包
 * - `hiltRule.inject()` の自動呼び出し
 *
 * ## 使用方法:
 *
 * ```kotlin
 * @HiltAndroidTest
 * @UninstallModules(AppModule::class) // 本番のDIモジュールを除外
 * class MyScreenTest {
 *
 *     // このルールを宣言するだけで、HiltとComposeの準備が整う
 *     @get:Rule
 *     val testRule = ヒルトコンポーズテストルール(this)
 *
 *     // @BindValueを使って、テストしたいRepositoryのモックをDIコンテナに提供
 *     @BindValue
 *     @JvmField
 *     val mockAnnictRepository: AnnictRepository = mockk(relaxed = true)
 *
 *     @Test
 *     fun myTest() {
 *         // ルール経由でComposeTestRuleにアクセス
 *         testRule.composeTestRule.setContent {
 *             // ... Test Target Composable
 *         }
 *         // ... アサーション ...
 *     }
 * }
 * ```
 */
class ヒルトコンポーズテストルール(testInstance: Any) : TestRule {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val hiltRule = HiltAndroidRule(testInstance)

    override fun apply(base: Statement, description: Description): Statement = RuleChain
        // HiltAndroidRuleを最も外側のルールとして適用
        .outerRule(hiltRule)
        // 次に、Hiltのインジェクションを実行する中間ルールを適用
        .around { statement, _ ->
            object : Statement() {
                override fun evaluate() {
                    // @BindValueなどを有効にするためにインジェクションを実行
                    hiltRule.inject()
                    // 内側のルール（ComposeTestRule）へ進む
                    statement.evaluate()
                }
            }
        }
        // 最も内側のルールとしてComposeTestRuleを適用
        .around(composeTestRule)
        .apply(base, description)
}
