package com.zeroday.zdscript

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NexusScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val authorId: String,
    val code: String,
    val description: String = "",
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val isPublic: Boolean = false,
    val price: Long = 0,
    val requiredLevel: Int = 1,
    val validated: Boolean = false,
    val validationErrors: List<String> = emptyList()
)

sealed class Token(val type: TokenType, open val lexeme: String, open val line: Int, open val column: Int)
data class IdentifierToken(val name: String, val lineNumber: Int, val columnNumber: Int) : Token(TokenType.IDENTIFIER, name, lineNumber, columnNumber)
data class NumberToken(val raw: String, val value: Double, val lineNumber: Int, val columnNumber: Int) : Token(TokenType.NUMBER, raw, lineNumber, columnNumber)
data class StringToken(val raw: String, val value: String, val lineNumber: Int, val columnNumber: Int) : Token(TokenType.STRING, raw, lineNumber, columnNumber)
data class KeywordToken(val word: String, val keyword: Keyword, val lineNumber: Int, val columnNumber: Int) : Token(TokenType.KEYWORD, word, lineNumber, columnNumber)
data class OperatorToken(val sym: String, val op: Operator, val lineNumber: Int, val columnNumber: Int) : Token(TokenType.OPERATOR, sym, lineNumber, columnNumber)
data class PunctuationToken(val symbol: String, val punct: Punctuation, val lineNumber: Int, val columnNumber: Int) : Token(TokenType.PUNCTUATION, symbol, lineNumber, columnNumber)

enum class TokenType { IDENTIFIER, NUMBER, STRING, KEYWORD, OPERATOR, PUNCTUATION, EOF }

enum class Keyword(val display: String) {
    FUNC("func"), SET("set"), IF("if"), ELSE("else"), FOR("for"), WHILE("while"),
    RETURN("return"), NEW("new"), RUN("run"), TRUE("true"), FALSE("false"),
    BREAK("break"), CONTINUE("continue")
}

enum class Operator(val sym: String) {
    PLUS("+"), MINUS("-"), MULT("*"), DIV("/"), ASSIGN("="),
    EQ("=="), NEQ("!="), LT("<"), GT(">"), LTE("<="), GTE(">="),
    AND("&&"), OR("||"), NOT("!"), DOT(".")
}

enum class Punctuation(val sym: String) {
    LPAREN("("), RPAREN(")"), LBRACE("{"), RBRACE("}"),
    LBRACKET("["), RBRACKET("]"), COMMA(","), SEMICOLON(";"), COLON(":"), ARROW("->")
}

sealed class ASTNode
data class ProgramNode(val statements: List<ASTNode>) : ASTNode()
data class VarDeclNode(val name: String, val value: ExpressionNode, val line: Int) : ASTNode()
data class FuncDeclNode(val name: String, val params: List<String>, val body: List<ASTNode>, val line: Int) : ASTNode()
data class IfNode(val condition: ExpressionNode, val thenBranch: List<ASTNode>, val elseBranch: List<ASTNode>, val line: Int) : ASTNode()
data class ForNode(val varName: String, val iterable: ExpressionNode, val body: List<ASTNode>, val line: Int) : ASTNode()
data class WhileNode(val condition: ExpressionNode, val body: List<ASTNode>, val line: Int) : ASTNode()
data class ReturnNode(val value: ExpressionNode?, val line: Int) : ASTNode()
data class RunNode(val scriptName: ExpressionNode, val args: List<ExpressionNode>, val line: Int) : ASTNode()
data class ExpressionStmtNode(val expr: ExpressionNode, val line: Int) : ASTNode()

sealed class ExpressionNode
data class LiteralExpr(val value: NexusValue) : ExpressionNode()
data class VariableExpr(val name: String) : ExpressionNode()
data class BinaryExpr(val left: ExpressionNode, val op: Operator, val right: ExpressionNode) : ExpressionNode()
data class UnaryExpr(val op: Operator, val right: ExpressionNode) : ExpressionNode()
data class CallExpr(val callee: ExpressionNode, val args: List<ExpressionNode>, val line: Int) : ExpressionNode()
data class GetAttrExpr(val obj: ExpressionNode, val attr: String) : ExpressionNode()
data class NewObjectExpr(val className: String, val args: List<ExpressionNode>, val line: Int) : ExpressionNode()

sealed class NexusValue {
    data class NumVal(val value: Double) : NexusValue()
    data class StrVal(val value: String) : NexusValue()
    data class BoolVal(val value: Boolean) : NexusValue()
    data class ObjRef(val obj: NexusObject) : NexusValue()
    object NullVal : NexusValue()
    data class ArrayVal(val elements: MutableList<NexusValue>) : NexusValue()

    fun asString(): String = when (this) {
        is NumVal -> if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
        is StrVal -> value
        is BoolVal -> value.toString()
        is ObjRef -> "[Object ${obj.typeName}]"
        is NullVal -> "null"
        is ArrayVal -> "[${elements.joinToString(", ") { it.asString() }}]"
    }

    fun asDouble(): Double = when (this) {
        is NumVal -> value
        is StrVal -> value.toDoubleOrNull() ?: 0.0
        is BoolVal -> if (value) 1.0 else 0.0
        else -> 0.0
    }

    fun isTruthy(): Boolean = when (this) {
        is NumVal -> value != 0.0
        is StrVal -> value.isNotEmpty()
        is BoolVal -> value
        is NullVal -> false
        is ObjRef -> true
        is ArrayVal -> elements.isNotEmpty()
    }
}

open class NexusObject(val typeName: String) {
    open val properties = mutableMapOf<String, NexusValue>()
    open val methods = mutableMapOf<String, (List<NexusValue>) -> NexusValue>()

    fun get(attr: String): NexusValue? = properties[attr] ?: NexusValue.NullVal
    fun set(attr: String, value: NexusValue) { properties[attr] = value }

    open fun call(method: String, args: List<NexusValue>): NexusValue =
        methods[method]?.invoke(args) ?: NexusValue.NullVal
}

class IPObject(ip: String) : NexusObject("IP_Object") {
    val address: String = ip
    var isOnline: Boolean = false
    var openPorts: MutableList<Int> = mutableListOf()
    var os: String = "Unknown"

    init {
        properties["address"] = NexusValue.StrVal(ip)
        properties["isOnline"] = NexusValue.BoolVal(false)
        properties["openPorts"] = NexusValue.ArrayVal(mutableListOf())
        properties["os"] = NexusValue.StrVal("Unknown")

        methods["scan"] = this::scanMethod
        methods["ping"] = this::pingMethod
        methods["hasPort"] = this::hasPortMethod
    }

    private fun scanMethod(args: List<NexusValue>): NexusValue {
        isOnline = true
        openPorts = listOf(22, 80, 443, 8080).filter { Math.random() > 0.3 }.toMutableList()
        os = listOf("Linux 5.15", "Windows Server 2022", "FreeBSD 13", "macOS Ventura").random()
        properties["isOnline"] = NexusValue.BoolVal(true)
        properties["openPorts"] = NexusValue.ArrayVal(openPorts.map { NexusValue.NumVal(it.toDouble()) }.toMutableList())
        properties["os"] = NexusValue.StrVal(os)
        return NexusValue.StrVal("Scanned $address: OS=$os, Ports=${openPorts.joinToString()}")
    }

    private fun pingMethod(args: List<NexusValue>): NexusValue {
        val rtt = (1..50).random()
        return NexusValue.StrVal("Ping to $address: ${rtt}ms")
    }

    private fun hasPortMethod(args: List<NexusValue>): NexusValue {
        val port = args.firstOrNull()?.asDouble()?.toInt() ?: return NexusValue.BoolVal(false)
        return NexusValue.BoolVal(port in openPorts)
    }
}

class PortObject(port: Int, protocol: String = "tcp") : NexusObject("Port_Object") {
    val number: Int = port
    val protocol: String = protocol
    var isOpen: Boolean = false
    var service: String = "Unknown"

    init {
        properties["number"] = NexusValue.NumVal(port.toDouble())
        properties["protocol"] = NexusValue.StrVal(protocol)
        properties["isOpen"] = NexusValue.BoolVal(false)
        properties["service"] = NexusValue.StrVal("Unknown")
    }
}

class ExploitObject(name: String, power: Int = 5) : NexusObject("Exploit_Object") {
    val exploitName: String = name
    val power: Int = power

    init {
        properties["name"] = NexusValue.StrVal(name)
        properties["power"] = NexusValue.NumVal(power.toDouble())

        methods["inject"] = this::injectMethod
        methods["analyze"] = this::analyzeMethod
    }

    private fun injectMethod(args: List<NexusValue>): NexusValue {
        val target = args.firstOrNull()
        val targetName = when (target) {
            is NexusValue.ObjRef -> target.obj.typeName
            is NexusValue.StrVal -> target.value
            else -> "Unknown"
        }
        val success = Math.random() > 0.3
        return NexusValue.StrVal(
            if (success) "Exploit '$exploitName' injected into $targetName! SYSTEM COMPROMISED"
            else "Exploit '$exploitName' failed against $targetName. Target may be patched."
        )
    }

    private fun analyzeMethod(args: List<NexusValue>): NexusValue {
        return NexusValue.StrVal("Analyzing $exploitName (Power: $power)... Suitable for targets with security level ≤ $power")
    }
}

data class ScriptModule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val authorId: String,
    val code: String,
    val description: String = "",
    val args: List<String> = emptyList(),
    val returnType: String = "auto",
    val validated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val downloads: Int = 0,
    val rating: Double = 0.0
)

data class MarketplaceListing(
    val id: String = UUID.randomUUID().toString(),
    val moduleId: String,
    val sellerId: String,
    val price: Long,
    val listedAt: Long = System.currentTimeMillis(),
    val sales: Int = 0
)

data class KnowledgeFragment(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: KnowledgeCategory,
    val requiredFragments: List<String> = emptyList(),
    val unlocksCommand: String? = null,
    val rarity: String = "common",
    val sourceHint: String = ""
)

enum class KnowledgeCategory {
    RECONNAISSANCE, EXPLOITATION, DEFENSE, CRYPTOGRAPHY, NETWORKING, SYSTEM, STEALTH, GENERAL
}
