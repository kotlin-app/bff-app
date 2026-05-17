package com.example.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// アプリケーションのエントリーポイント
// @SpringBootApplication により、コンポーネントスキャン・自動設定が有効になる
@SpringBootApplication
class BffApplication

fun main(args: Array<String>) {
    runApplication<BffApplication>(*args)
}
