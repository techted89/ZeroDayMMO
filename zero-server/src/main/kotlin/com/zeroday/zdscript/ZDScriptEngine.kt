package com.zeroday.zdscript

import com.zeroday.model.GameCommand
import com.zeroday.model.CommandRegistry

class NexusScriptEngine(
    private val builtinCommands: Map<String, GameCommand> = CommandRegistry.commandMap
) {
    fun execute(script: NexusScript, context: ScriptContext): ScriptResult {
        return try {
            val tokens = Lexer(script.code).tokenize()
            val ast = Parser(tokens).parse()
            val interpreter = Interpreter(context, builtinCommands)
            val result = interpreter.interpret(ast)
            ScriptResult(
                success = true,
                output = result.first,
                returnValue = result.second,
                console = interpreter.console.joinToString("\n"),
                variables = context.variables.mapValues { it.value.asString() },
                executionTimeMs = 0
            )
        } catch (e: ScriptError) {
            ScriptResult(
                success = false,
                error = e.message ?: "Unknown error",
                console = "",
                executionTimeMs = 0
            )
        } catch (e: Exception) {
            ScriptResult(
                success = false,
                error = "Runtime error: ${e.message}",
                console = "",
                executionTimeMs = 0
            )
        }
    }

    fun validate(code: String): ValidationResult {
        return try {
            val tokens = Lexer(code).tokenize()
            Parser(tokens).parse()
            ValidationResult(valid = true)
        } catch (e: ScriptError) {
            ValidationResult(valid = false, errors = listOf(e.message ?: "Parse error"))
        } catch (e: Exception) {
            ValidationResult(valid = false, errors = listOf("Validation error: ${e.message}"))
        }
    }

    data class ScriptResult(
        val success: Boolean,
        val output: String = "",
        val returnValue: NexusValue = NexusValue.NullVal,
        val console: String = "",
        val variables: Map<String, String> = emptyMap(),
        val error: String = "",
        val executionTimeMs: Long = 0
    )

    data class ValidationResult(
        val valid: Boolean,
        val errors: List<String> = emptyList()
    )
}

class ScriptContext(
    val playerId: String,
    val playerLevel: Int = 1
) {
    val variables = mutableMapOf<String, NexusValue>()
    val functions = mutableMapOf<String, FuncDeclNode>()
    val objects = mutableMapOf<String, NexusObject>()
    var output = StringBuilder()
    var returnValue: NexusValue = NexusValue.NullVal
}

class Lexer(private val source: String) {
    private var start = 0
    private var current = 0
    private var lineNumber = 1
    private var columnNumber = 1
    private val tokens = mutableListOf<Token>()
    private val keywords = Keyword.entries.associateBy { it.display }

    fun tokenize(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }
        tokens.add(PunctuationToken("", Punctuation.SEMICOLON, lineNumber, columnNumber))
        return tokens
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(', ')' -> addPunct(if (c == '(') Punctuation.LPAREN else Punctuation.RPAREN)
            '{', '}' -> addPunct(if (c == '{') Punctuation.LBRACE else Punctuation.RBRACE)
            '[', ']' -> addPunct(if (c == '[') Punctuation.LBRACKET else Punctuation.RBRACKET)
            ',' -> addPunct(Punctuation.COMMA)
            ';' -> addPunct(Punctuation.SEMICOLON)
            ':' -> addPunct(if (match('>')) Punctuation.ARROW else Punctuation.COLON)
            '+' -> addOp(Operator.PLUS)
            '-' -> {
                if (match('>')) {
                    addPunct(Punctuation.ARROW)
                } else {
                    addOp(Operator.MINUS)
                }
            }
            '*' -> addOp(Operator.MULT)
            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd()) advance()
                } else addOp(Operator.DIV)
            }
            '!' -> addOp(if (match('=')) Operator.NEQ else Operator.NOT)
            '=' -> addOp(if (match('=')) Operator.EQ else Operator.ASSIGN)
            '<' -> addOp(if (match('=')) Operator.LTE else Operator.LT)
            '>' -> addOp(if (match('=')) Operator.GTE else Operator.GT)
            '&' -> { if (match('&')) addOp(Operator.AND) else error("Expected '&&'") }
            '|' -> { if (match('|')) addOp(Operator.OR) else error("Expected '||'") }
            '.' -> addOp(Operator.DOT)
            '"' -> scanString()
            ' ', '\t', '\r' -> {}
            '\n' -> { lineNumber++; columnNumber = 1 }
            else -> when {
                c.isDigit() -> scanNumber()
                c.isLetter() || c == '_' -> scanIdentifier()
                else -> error("Unexpected character '$c'")
            }
        }
    }

    private fun scanString() {
        val sb = StringBuilder()
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') lineNumber++
            sb.append(advance())
        }
        if (isAtEnd()) error("Unterminated string")
        advance()
        tokens.add(StringToken(sb.toString(), sb.toString(), lineNumber, columnNumber))
    }

    private fun scanNumber() {
        val sb = StringBuilder()
        while (peek()?.isDigit() == true) sb.append(advance())
        if (peek() == '.' && peek(1)?.isDigit() == true) {
            sb.append(advance())
            while (peek()?.isDigit() == true) sb.append(advance())
        }
        tokens.add(NumberToken(sb.toString(), sb.toString().toDouble(), lineNumber, columnNumber))
    }

    private fun scanIdentifier() {
        val sb = StringBuilder()
        while (peek()?.isLetterOrDigit() == true || peek() == '_') sb.append(advance())
        val word = sb.toString()
        val keyword = keywords[word]
        if (keyword != null) tokens.add(KeywordToken(word, keyword, lineNumber, columnNumber))
        else tokens.add(IdentifierToken(word, lineNumber, columnNumber))
    }

    private fun advance(): Char = source[current++].also { columnNumber++ }
    private fun peek(offset: Int = 0): Char? = if (current + offset < source.length) source[current + offset] else null
    private fun match(expected: Char): Boolean {
        if (isAtEnd() || source[current] != expected) return false
        current++; columnNumber++; return true
    }
    private fun isAtEnd() = current >= source.length
    private fun addPunct(p: Punctuation) = tokens.add(PunctuationToken(p.sym, p, lineNumber, columnNumber))
    private fun addOp(o: Operator) = tokens.add(OperatorToken(o.sym, o, lineNumber, columnNumber))
    private fun error(msg: String): Nothing = throw ScriptError(msg, lineNumber, columnNumber)
}

class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): ProgramNode {
        val statements = mutableListOf<ASTNode>()
        while (!isAtEnd()) {
            val stmt = declaration()
            if (stmt != null) statements.add(stmt)
        }
        return ProgramNode(statements)
    }

    private fun declaration(): ASTNode? {
        return try {
            when {
                match(Keyword.FUNC) -> funcDecl()
                match(Keyword.SET) -> varDecl()
                else -> statement()
            }
        } catch (e: ScriptError) {
            synchronize()
            null
        }
    }

    private fun funcDecl(): ASTNode {
        val line = previous().line
        val name = consume(TokenType.IDENTIFIER, "Expected function name").lexeme
        consume(TokenType.PUNCTUATION, "Expected '('") { it is PunctuationToken && it.punct == Punctuation.LPAREN }
        val params = mutableListOf<String>()
        if (!check(TokenType.PUNCTUATION) || (peek() as? PunctuationToken)?.punct != Punctuation.RPAREN) {
            do {
                params.add(consume(TokenType.IDENTIFIER, "Expected parameter name").lexeme)
            } while (match(Punctuation.COMMA))
        }
        consume(TokenType.PUNCTUATION, "Expected ')'") { it is PunctuationToken && it.punct == Punctuation.RPAREN }
        consume(TokenType.PUNCTUATION, "Expected '{'") { it is PunctuationToken && it.punct == Punctuation.LBRACE }
        val body = mutableListOf<ASTNode>()
        while (!check(TokenType.PUNCTUATION) || (peek() as? PunctuationToken)?.punct != Punctuation.RBRACE) {
            if (isAtEnd()) throw error(peek(), "Unterminated function body")
            body.add(declaration() ?: continue)
        }
        consume(TokenType.PUNCTUATION, "Expected '}'") { it is PunctuationToken && it.punct == Punctuation.RBRACE }
        return FuncDeclNode(name, params, body, line)
    }

    private fun varDecl(): ASTNode {
        val line = previous().line
        val name = consume(TokenType.IDENTIFIER, "Expected variable name").lexeme
        consume(TokenType.OPERATOR, "Expected '='") { it is OperatorToken && it.op == Operator.ASSIGN }
        val value = expression()
        consume(TokenType.PUNCTUATION, "Expected ';'") { it is PunctuationToken && it.punct == Punctuation.SEMICOLON }
        return VarDeclNode(name, value, line)
    }

    private fun statement(): ASTNode {
        val line = peek().line
        return when {
            match(Keyword.IF) -> ifStmt()
            match(Keyword.FOR) -> forStmt()
            match(Keyword.WHILE) -> whileStmt()
            match(Keyword.RETURN) -> returnStmt()
            match(Keyword.RUN) -> runStmt()
            else -> {
                val expr = expression()
                consume(TokenType.PUNCTUATION, "Expected ';'") { it is PunctuationToken && it.punct == Punctuation.SEMICOLON }
                ExpressionStmtNode(expr, line)
            }
        }
    }

    private fun ifStmt(): ASTNode {
        val line = previous().line
        consume(TokenType.PUNCTUATION, "Expected '('") { it is PunctuationToken && it.punct == Punctuation.LPAREN }
        val condition = expression()
        consume(TokenType.PUNCTUATION, "Expected ')'") { it is PunctuationToken && it.punct == Punctuation.RPAREN }
        consume(TokenType.PUNCTUATION, "Expected '{'") { it is PunctuationToken && it.punct == Punctuation.LBRACE }
        val thenBranch = block()
        val elseBranch = if (match(Keyword.ELSE)) {
            consume(TokenType.PUNCTUATION, "Expected '{'") { it is PunctuationToken && it.punct == Punctuation.LBRACE }
            block()
        } else emptyList()
        return IfNode(condition, thenBranch, elseBranch, line)
    }

    private fun forStmt(): ASTNode {
        val line = previous().line
        consume(TokenType.PUNCTUATION, "Expected '('") { it is PunctuationToken && it.punct == Punctuation.LPAREN }
        val varName = consume(TokenType.IDENTIFIER, "Expected variable name").lexeme
        consume(TokenType.KEYWORD, "Expected 'in'") { it is KeywordToken && it.keyword == Keyword.SET }
        val iterable = expression()
        consume(TokenType.PUNCTUATION, "Expected ')'") { it is PunctuationToken && it.punct == Punctuation.RPAREN }
        consume(TokenType.PUNCTUATION, "Expected '{'") { it is PunctuationToken && it.punct == Punctuation.LBRACE }
        val body = block()
        return ForNode(varName, iterable, body, line)
    }

    private fun whileStmt(): ASTNode {
        val line = previous().line
        consume(TokenType.PUNCTUATION, "Expected '('") { it is PunctuationToken && it.punct == Punctuation.LPAREN }
        val condition = expression()
        consume(TokenType.PUNCTUATION, "Expected ')'") { it is PunctuationToken && it.punct == Punctuation.RPAREN }
        consume(TokenType.PUNCTUATION, "Expected '{'") { it is PunctuationToken && it.punct == Punctuation.LBRACE }
        val body = block()
        return WhileNode(condition, body, line)
    }

    private fun returnStmt(): ASTNode {
        val line = previous().line
        val value = if (!check(TokenType.PUNCTUATION) || (peek() as? PunctuationToken)?.punct != Punctuation.SEMICOLON) {
            expression()
        } else null
        consume(TokenType.PUNCTUATION, "Expected ';'") { it is PunctuationToken && it.punct == Punctuation.SEMICOLON }
        return ReturnNode(value, line)
    }

    private fun runStmt(): ASTNode {
        val line = previous().line
        val scriptName = expression()
        consume(TokenType.PUNCTUATION, "Expected ';'") { it is PunctuationToken && it.punct == Punctuation.SEMICOLON }
        return RunNode(scriptName, emptyList(), line)
    }

    private fun block(): List<ASTNode> {
        val stmts = mutableListOf<ASTNode>()
        while (!check(TokenType.PUNCTUATION) || (peek() as? PunctuationToken)?.punct != Punctuation.RBRACE) {
            if (isAtEnd()) throw error(peek(), "Unterminated block")
            stmts.add(declaration() ?: continue)
        }
        consume(TokenType.PUNCTUATION, "Expected '}'") { it is PunctuationToken && it.punct == Punctuation.RBRACE }
        return stmts
    }

    private fun expression(): ExpressionNode = assignment()
    private fun assignment(): ExpressionNode {
        var expr = orExpr()
        if (match(Operator.ASSIGN)) {
            val value = assignment()
            if (expr is VariableExpr) expr = BinaryExpr(expr, Operator.ASSIGN, value)
            else throw error(peek(), "Invalid assignment target")
        }
        return expr
    }

    private fun orExpr(): ExpressionNode {
        var expr = andExpr()
        while (match(Operator.OR)) {
            val op = (previous() as OperatorToken).op
            val right = andExpr()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun andExpr(): ExpressionNode {
        var expr = equality()
        while (match(Operator.AND)) {
            val op = (previous() as OperatorToken).op
            val right = equality()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun equality(): ExpressionNode {
        var expr = comparison()
        while (match(Operator.EQ, Operator.NEQ)) {
            val op = (previous() as OperatorToken).op
            val right = comparison()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun comparison(): ExpressionNode {
        var expr = term()
        while (match(Operator.LT, Operator.GT, Operator.LTE, Operator.GTE)) {
            val op = (previous() as OperatorToken).op
            val right = term()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun term(): ExpressionNode {
        var expr = factor()
        while (match(Operator.PLUS, Operator.MINUS)) {
            val op = (previous() as OperatorToken).op
            val right = factor()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun factor(): ExpressionNode {
        var expr = unary()
        while (match(Operator.MULT, Operator.DIV)) {
            val op = (previous() as OperatorToken).op
            val right = unary()
            expr = BinaryExpr(expr, op, right)
        }
        return expr
    }

    private fun unary(): ExpressionNode {
        if (match(Operator.MINUS, Operator.NOT)) return UnaryExpr((previous() as OperatorToken).op, unary())
        return call()
    }

    private fun call(): ExpressionNode {
        var expr = primary()
        while (true) {
            if (match(Punctuation.LPAREN)) {
                val args = argumentList()
                consume(TokenType.PUNCTUATION, "Expected ')'") { it is PunctuationToken && it.punct == Punctuation.RPAREN }
                expr = CallExpr(expr, args, peek().line)
            } else if (match(Operator.DOT)) {
                val name = consume(TokenType.IDENTIFIER, "Expected property name").lexeme
                expr = GetAttrExpr(expr, name)
            } else break
        }
        return expr
    }

    private fun primary(): ExpressionNode {
        if (match(Keyword.TRUE)) return LiteralExpr(NexusValue.BoolVal(true))
        if (match(Keyword.FALSE)) return LiteralExpr(NexusValue.BoolVal(false))
        if (match(Keyword.NEW)) return newObjectExpr()
        if (match(Punctuation.LBRACKET)) {
            val elements = mutableListOf<ExpressionNode>()
            if (!check(TokenType.PUNCTUATION) || (peek() as? PunctuationToken)?.punct != Punctuation.RBRACKET) {
                do { elements.add(expression()) } while (match(Punctuation.COMMA))
            }
            consume(TokenType.PUNCTUATION, "Expected ']'") { it is PunctuationToken && it.punct == Punctuation.RBRACKET }
            return CallExpr(VariableExpr("array"), elements, peek().line)
        }
        if (match(Punctuation.LPAREN)) {
            val expr = expression()
            consume(TokenType.PUNCTUATION, "Expected ')'") { it is PunctuationToken && it.punct == Punctuation.RPAREN }
            return expr
        }
        return when (val token = advance()) {
            is NumberToken -> LiteralExpr(NexusValue.NumVal(token.value))
            is StringToken -> LiteralExpr(NexusValue.StrVal(token.value))
            is IdentifierToken -> VariableExpr(token.lexeme)
            else -> throw error(token, "Expected expression")
        }
    }

    private fun newObjectExpr(): ExpressionNode {
        val className = consume(TokenType.IDENTIFIER, "Expected class name").lexeme
        consume(TokenType.PUNCTUATION, "Expected '('") { it is PunctuationToken && it.punct == Punctuation.LPAREN }
        val args = argumentList()
        consume(TokenType.PUNCTUATION, "Expected ')'") { it is PunctuationToken && it.punct == Punctuation.RPAREN }
        return NewObjectExpr(className, args, peek().line)
    }

    private fun argumentList(): List<ExpressionNode> {
        val args = mutableListOf<ExpressionNode>()
        if (!check(TokenType.PUNCTUATION) || (peek() as? PunctuationToken)?.punct != Punctuation.RPAREN) {
            do { args.add(expression()) } while (match(Punctuation.COMMA))
        }
        return args
    }

    private fun advance(): Token { if (!isAtEnd()) current++; return previous() }
    private fun previous() = tokens[current - 1]
    private fun peek() = tokens[current]
    private fun isAtEnd(): Boolean {
        if (current >= tokens.size) return true
        val token = tokens[current]
        if (token is PunctuationToken && token.punct == Punctuation.SEMICOLON && current >= tokens.size - 1) {
            return true
        }
        return false
    }
    private fun check(type: TokenType) = !isAtEnd() && peek().type == type
    private fun match(vararg ops: Operator): Boolean {
        if (isAtEnd() || peek() !is OperatorToken) return false
        val op = (peek() as OperatorToken).op
        if (op in ops) { advance(); return true }
        return false
    }
    private fun match(punct: Punctuation): Boolean {
        if (isAtEnd() || peek() !is PunctuationToken) return false
        if ((peek() as PunctuationToken).punct == punct) { advance(); return true }
        return false
    }
    private fun match(keyword: Keyword): Boolean {
        if (isAtEnd() || peek() !is KeywordToken) return false
        if ((peek() as KeywordToken).keyword == keyword) { advance(); return true }
        return false
    }
    private fun consume(type: TokenType, msg: String, predicate: ((Token) -> Boolean)? = null): Token {
        if (check(type) && (predicate == null || predicate(peek()))) return advance()
        throw error(peek(), msg)
    }
    private fun error(token: Token, msg: String): ScriptError = ScriptError(msg, token.line, 0)
    private fun synchronize() {
        while (!isAtEnd()) {
            if (previous() is PunctuationToken && (previous() as PunctuationToken).punct == Punctuation.SEMICOLON) return
            advance()
        }
    }
}

class Interpreter(
    val context: ScriptContext,
    private val builtinCommands: Map<String, GameCommand>
) {
    val console = mutableListOf<String>()

    fun interpret(node: ASTNode): Pair<String, NexusValue> {
        return when (node) {
            is ProgramNode -> {
                node.statements.forEach { executeStmt(it) }
                Pair(context.output.toString(), context.returnValue)
            }
            else -> {
                executeStmt(node)
                Pair(context.output.toString(), context.returnValue)
            }
        }
    }

    private fun executeStmt(node: ASTNode) {
        when (node) {
            is ProgramNode -> node.statements.forEach { executeStmt(it) }
            is VarDeclNode -> {
                val value = evaluate(node.value)
                context.variables[node.name] = value
            }
            is FuncDeclNode -> context.functions[node.name] = node
            is IfNode -> {
                val cond = evaluate(node.condition)
                if (cond.isTruthy()) node.thenBranch.forEach { executeStmt(it) }
                else node.elseBranch.forEach { executeStmt(it) }
            }
            is ForNode -> {
                val iterable = evaluate(node.iterable)
                val elements = when (iterable) {
                    is NexusValue.ArrayVal -> iterable.elements
                    is NexusValue.StrVal -> iterable.value.map { NexusValue.StrVal(it.toString()) }.toMutableList()
                    else -> mutableListOf()
                }
                for (element in elements) {
                    context.variables[node.varName] = element
                    node.body.forEach { executeStmt(it) }
                }
            }
            is WhileNode -> {
                while (evaluate(node.condition).isTruthy()) {
                    node.body.forEach { executeStmt(it) }
                }
            }
            is ReturnNode -> {
                context.returnValue = if (node.value != null) evaluate(node.value) else NexusValue.NullVal
            }
            is RunNode -> {
                val scriptName = evaluate(node.scriptName)
                val scriptStr = scriptName.asString()
                console.add("[RUN] Attempting to execute script: $scriptStr")
                context.output.appendLine("[RUN] Script '$scriptStr' executed (simulated)")
            }
            is ExpressionStmtNode -> evaluate(node.expr)
        }
    }

    private fun evaluate(expr: ExpressionNode): NexusValue {
        return when (expr) {
            is LiteralExpr -> expr.value
            is VariableExpr -> {
                val name = expr.name
                when {
                    context.variables.containsKey(name) -> context.variables[name]!!
                    context.functions.containsKey(name) -> NexusValue.StrVal("[Function: $name]")
                    else -> {
                        context.variables[name] = NexusValue.NullVal
                        NexusValue.NullVal
                    }
                }
            }
            is BinaryExpr -> evaluateBinary(expr)
            is UnaryExpr -> {
                val right = evaluate(expr.right)
                when (expr.op) {
                    Operator.MINUS -> NexusValue.NumVal(-right.asDouble())
                    Operator.NOT -> NexusValue.BoolVal(!right.isTruthy())
                    else -> NexusValue.NullVal
                }
            }
            is CallExpr -> evaluateCall(expr)
            is GetAttrExpr -> {
                val obj = evaluate(expr.obj)
                if (obj is NexusValue.ObjRef) obj.obj.get(expr.attr) ?: NexusValue.NullVal
                else NexusValue.NullVal
            }
            is NewObjectExpr -> {
                val args = expr.args.map { evaluate(it) }
                val obj = when (expr.className) {
                    "IP_Object" -> IPObject(args.firstOrNull()?.asString() ?: "0.0.0.0")
                    "Port_Object" -> PortObject(
                        args.firstOrNull()?.asDouble()?.toInt() ?: 0,
                        args.getOrNull(1)?.asString() ?: "tcp"
                    )
                    "Exploit_Object" -> ExploitObject(
                        args.firstOrNull()?.asString() ?: "generic_exploit",
                        args.getOrNull(1)?.asDouble()?.toInt() ?: 5
                    )
                    else -> NexusObject(expr.className)
                }
                NexusValue.ObjRef(obj)
            }
        }
    }

    private fun evaluateBinary(expr: BinaryExpr): NexusValue {
        return when (expr.op) {
            Operator.ASSIGN -> {
                if (expr.left is VariableExpr) {
                    val value = evaluate(expr.right)
                    context.variables[(expr.left as VariableExpr).name] = value
                    value
                } else NexusValue.NullVal
            }
            Operator.PLUS, Operator.MINUS, Operator.MULT, Operator.DIV -> {
                val left = evaluate(expr.left).asDouble()
                val right = evaluate(expr.right).asDouble()
                val result = when (expr.op) {
                    Operator.PLUS -> left + right
                    Operator.MINUS -> left - right
                    Operator.MULT -> left * right
                    Operator.DIV -> if (right != 0.0) left / right else 0.0
                    else -> 0.0
                }
                NexusValue.NumVal(result)
            }
            Operator.EQ -> NexusValue.BoolVal(evaluate(expr.left).asString() == evaluate(expr.right).asString())
            Operator.NEQ -> NexusValue.BoolVal(evaluate(expr.left).asString() != evaluate(expr.right).asString())
            Operator.LT -> NexusValue.BoolVal(evaluate(expr.left).asDouble() < evaluate(expr.right).asDouble())
            Operator.GT -> NexusValue.BoolVal(evaluate(expr.left).asDouble() > evaluate(expr.right).asDouble())
            Operator.LTE -> NexusValue.BoolVal(evaluate(expr.left).asDouble() <= evaluate(expr.right).asDouble())
            Operator.GTE -> NexusValue.BoolVal(evaluate(expr.left).asDouble() >= evaluate(expr.right).asDouble())
            Operator.AND -> NexusValue.BoolVal(evaluate(expr.left).isTruthy() && evaluate(expr.right).isTruthy())
            Operator.OR -> NexusValue.BoolVal(evaluate(expr.left).isTruthy() || evaluate(expr.right).isTruthy())
            Operator.DOT -> NexusValue.NullVal
            Operator.NOT -> NexusValue.BoolVal(!evaluate(expr.right).isTruthy())
        }
    }

    private fun evaluateCall(expr: CallExpr): NexusValue {
        val args = expr.args.map { evaluate(it) }
        return when (val callee = expr.callee) {
            is VariableExpr -> {
                val name = callee.name
                val func = context.functions[name]
                when {
                    func != null -> {
                        val oldVars = context.variables.toMutableMap()
                        func.params.forEachIndexed { i, p ->
                            context.variables[p] = if (i < args.size) args[i] else NexusValue.NullVal
                        }
                        func.body.forEach { executeStmt(it) }
                        val ret = context.returnValue
                        context.returnValue = NexusValue.NullVal
                        context.variables.clear()
                        context.variables.putAll(oldVars)
                        ret
                    }
                    name == "print" -> {
                        val msg = args.joinToString(", ") { it.asString() }
                        console.add(msg)
                        context.output.appendLine(msg)
                        NexusValue.NullVal
                    }
                    name == "printline" -> {
                        val msg = args.joinToString(", ") { it.asString() }
                        console.add(msg)
                        context.output.appendLine(msg)
                        NexusValue.NullVal
                    }
                    name == "sleep" -> NexusValue.NullVal
                    name in builtinCommands -> {
                        val cmd = builtinCommands[name]!!
                        context.output.appendLine("[${name.uppercase()}] ${cmd.description}")
                        context.output.appendLine("[Syntax: ${cmd.syntax}]")
                        context.output.appendLine("[Cost: ${cmd.cpuCost} CPU / ${cmd.ramCost} RAM]")
                        NexusValue.StrVal("[Executed: $name]")
                    }
                    else -> NexusValue.NullVal
                }
            }
            is GetAttrExpr -> {
                val obj = evaluate(callee.obj)
                if (obj is NexusValue.ObjRef) obj.obj.call(callee.attr, args)
                else NexusValue.NullVal
            }
            else -> NexusValue.NullVal
        }
    }
}

class ScriptError(message: String, val line: Int, val column: Int) : Exception("Line $line, Col $column: $message")
