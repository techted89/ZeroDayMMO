using System.Text;
using ZeroDayMMO.Shared.Models;

namespace ZeroDayMMO.Server.Services;

public record NexusScript(
    string Name,
    string AuthorId,
    string Code,
    string Description = "",
    string Id = "",
    int Version = 1,
    long CreatedAt = 0,
    bool IsPublic = false,
    long Price = 0,
    int RequiredLevel = 1,
    bool Validated = false,
    List<string>? ValidationErrors = null
)
{
    public string Id { get; init; } = string.IsNullOrEmpty(Id) ? Guid.NewGuid().ToString() : Id;
    public long CreatedAt { get; init; } = CreatedAt == 0 ? DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() : CreatedAt;
    public List<string> ValidationErrors { get; init; } = ValidationErrors ?? new List<string>();
}

public abstract class NexusValue
{
    public static NexusValue NumVal(double value) => new NumValue(value);
    public static NexusValue StrVal(string value) => new StrValue(value);
    public static NexusValue BoolVal(bool value) => new BoolValue(value);
    public static NexusValue ObjRef(NexusObject obj) => new ObjValue(obj);
    public static NexusValue ArrayVal(List<NexusValue> elements) => new ArrayValue(elements);
    public static NexusValue NullVal() => NullValue.Instance;

    public virtual string AsString() => "null";
    public virtual double AsDouble() => 0.0;
    public virtual bool IsTruthy() => false;

    public class NumValue : NexusValue
    {
        public double Value { get; }
        public NumValue(double value) { Value = value; }
        public override string AsString() => Value == (long)Value ? ((long)Value).ToString() : Value.ToString();
        public override double AsDouble() => Value;
        public override bool IsTruthy() => Value != 0.0;
    }

    public class StrValue : NexusValue
    {
        public string Value { get; }
        public StrValue(string value) { Value = value; }
        public override string AsString() => Value;
        public override double AsDouble() => double.TryParse(Value, out var d) ? d : 0.0;
        public override bool IsTruthy() => Value.Length > 0;
    }

    public class BoolValue : NexusValue
    {
        public bool Value { get; }
        public BoolValue(bool value) { Value = value; }
        public override string AsString() => Value.ToString().ToLower();
        public override double AsDouble() => Value ? 1.0 : 0.0;
        public override bool IsTruthy() => Value;
    }

    public class ObjValue : NexusValue
    {
        public NexusObject Obj { get; }
        public ObjValue(NexusObject obj) { Obj = obj; }
        public override string AsString() => $"[Object {Obj.TypeName}]";
        public override bool IsTruthy() => true;
    }

    public class ArrayValue : NexusValue
    {
        public List<NexusValue> Elements { get; }
        public ArrayValue(List<NexusValue> elements) { Elements = elements; }
        public override string AsString() => $"[{string.Join(", ", Elements.Select(e => e.AsString()))}]";
        public override bool IsTruthy() => Elements.Count > 0;
    }

    public class NullValue : NexusValue
    {
        private NullValue() { }
        public static readonly NullValue Instance = new();
        public override string AsString() => "null";
    }
}

public class NexusObject
{
    public string TypeName { get; }
    public Dictionary<string, NexusValue> Properties { get; } = new();
    public Dictionary<string, Func<List<NexusValue>, NexusValue>> Methods { get; } = new();

    public NexusObject(string typeName)
    {
        TypeName = typeName;
    }

    public NexusValue Get(string attr) =>
        Properties.TryGetValue(attr, out var val) ? val : NexusValue.NullVal();

    public void Set(string attr, NexusValue value)
    {
        Properties[attr] = value;
    }

    public virtual NexusValue Call(string method, List<NexusValue> args) =>
        Methods.TryGetValue(method, out var fn) ? fn(args) : NexusValue.NullVal();
}

public class IPObject : NexusObject
{
    public string Address { get; }
    public bool IsOnline { get; set; }
    public List<int> OpenPorts { get; set; } = new();
    public string Os { get; set; } = "Unknown";

    public IPObject(string ip) : base("IP_Object")
    {
        Address = ip;
        Properties["address"] = NexusValue.StrVal(ip);
        Properties["isOnline"] = NexusValue.BoolVal(false);
        Properties["openPorts"] = NexusValue.ArrayVal(new List<NexusValue>());
        Properties["os"] = NexusValue.StrVal("Unknown");
        Methods["scan"] = ScanMethod;
        Methods["ping"] = PingMethod;
        Methods["hasPort"] = HasPortMethod;
    }

    private NexusValue ScanMethod(List<NexusValue> args)
    {
        IsOnline = true;
        OpenPorts = new List<int> { 22, 80, 443, 8080 }
            .Where(_ => Random.Shared.NextDouble() > 0.3)
            .ToList();
        Os = new[] { "Linux 5.15", "Windows Server 2022", "FreeBSD 13", "macOS Ventura" }[Random.Shared.Next(4)];
        Properties["isOnline"] = NexusValue.BoolVal(true);
        Properties["openPorts"] = NexusValue.ArrayVal(OpenPorts.Select(p => NexusValue.NumVal(p)).ToList());
        Properties["os"] = NexusValue.StrVal(Os);
        return NexusValue.StrVal($"Scanned {Address}: OS={Os}, Ports={string.Join(", ", OpenPorts)}");
    }

    private NexusValue PingMethod(List<NexusValue> args)
    {
        var rtt = Random.Shared.Next(1, 51);
        return NexusValue.StrVal($"Ping to {Address}: {rtt}ms");
    }

    private NexusValue HasPortMethod(List<NexusValue> args)
    {
        if (args.Count == 0) return NexusValue.BoolVal(false);
        var port = (int)args[0].AsDouble();
        return NexusValue.BoolVal(OpenPorts.Contains(port));
    }
}

public class PortObject : NexusObject
{
    public int Number { get; }
    public string Protocol { get; }
    public bool IsOpen { get; set; }
    public string Service { get; set; } = "Unknown";

    public PortObject(int port, string protocol = "tcp") : base("Port_Object")
    {
        Number = port;
        Protocol = protocol;
        Properties["number"] = NexusValue.NumVal(port);
        Properties["protocol"] = NexusValue.StrVal(protocol);
        Properties["isOpen"] = NexusValue.BoolVal(false);
        Properties["service"] = NexusValue.StrVal("Unknown");
    }
}

public class ExploitObject : NexusObject
{
    public string ExploitName { get; }
    public int Power { get; }

    public ExploitObject(string name, int power = 5) : base("Exploit_Object")
    {
        ExploitName = name;
        Power = power;
        Properties["name"] = NexusValue.StrVal(name);
        Properties["power"] = NexusValue.NumVal(power);
        Methods["inject"] = InjectMethod;
        Methods["analyze"] = AnalyzeMethod;
    }

    private NexusValue InjectMethod(List<NexusValue> args)
    {
        var target = args.Count > 0 ? args[0] : null;
        var targetName = target switch
        {
            NexusValue.ObjValue ov => ov.Obj.TypeName,
            NexusValue.StrValue sv => sv.Value,
            _ => "Unknown"
        };
        var success = Random.Shared.NextDouble() > 0.3;
        return NexusValue.StrVal(
            success
                ? $"Exploit '{ExploitName}' injected into {targetName}! SYSTEM COMPROMISED"
                : $"Exploit '{ExploitName}' failed against {targetName}. Target may be patched."
        );
    }

    private NexusValue AnalyzeMethod(List<NexusValue> args)
    {
        return NexusValue.StrVal($"Analyzing {ExploitName} (Power: {Power})... Suitable for targets with security level <= {Power}");
    }
}

public class ScriptContext
{
    public string PlayerId { get; }
    public int PlayerLevel { get; set; }
    public Dictionary<string, NexusValue> Variables { get; } = new();
    public Dictionary<string, FuncDeclNode> Functions { get; } = new();
    public Dictionary<string, NexusObject> Objects { get; } = new();
    public StringBuilder Output { get; } = new();
    public NexusValue ReturnValue { get; set; } = NexusValue.NullVal();
    public int OpBudget { get; set; } = 5000;
    public int NestingDepth { get; set; } = 0;
    public const int MaxNesting = 100;

    public ScriptContext(string playerId, int playerLevel)
    {
        PlayerId = playerId;
        PlayerLevel = playerLevel;
    }
}

public class ScriptError : Exception
{
    public int Line { get; }
    public int Column { get; }

    public ScriptError(string message, int line, int column)
        : base($"Line {line}, Col {column}: {message}")
    {
        Line = line;
        Column = column;
    }
}

internal enum TokenType { IDENTIFIER, NUMBER, STRING, KEYWORD, OPERATOR, PUNCTUATION, EOF }

public enum Keyword
{
    FUNC, SET, IF, ELSE, FOR, WHILE, RETURN, NEW, RUN, TRUE, FALSE, BREAK, CONTINUE
}

public enum Operator
{
    PLUS, MINUS, MULT, DIV, ASSIGN, EQ, NEQ, LT, GT, LTE, GTE, AND, OR, NOT, DOT
}

internal enum Punctuation
{
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET, COMMA, SEMICOLON, COLON, ARROW
}

internal abstract record Token(TokenType Type, string Lexeme, int Line, int Column);
internal record IdentifierToken(string Name, int LineNumber, int ColumnNumber) : Token(TokenType.IDENTIFIER, Name, LineNumber, ColumnNumber);
internal record NumberToken(string Raw, double Value, int LineNumber, int ColumnNumber) : Token(TokenType.NUMBER, Raw, LineNumber, ColumnNumber);
internal record StringToken(string Raw, string Value, int LineNumber, int ColumnNumber) : Token(TokenType.STRING, Raw, LineNumber, ColumnNumber);
internal record KeywordToken(string Word, Keyword Keyword, int LineNumber, int ColumnNumber) : Token(TokenType.KEYWORD, Word, LineNumber, ColumnNumber);
internal record OperatorToken(string Sym, Operator Op, int LineNumber, int ColumnNumber) : Token(TokenType.OPERATOR, Sym, LineNumber, ColumnNumber);
internal record PunctuationToken(string Symbol, Punctuation Punct, int LineNumber, int ColumnNumber) : Token(TokenType.PUNCTUATION, Symbol, LineNumber, ColumnNumber);

public abstract record ASTNode;
public record ProgramNode(List<ASTNode> Statements) : ASTNode;
public record VarDeclNode(string Name, ExpressionNode Value, int Line) : ASTNode;
public record FuncDeclNode(string Name, List<string> Params, List<ASTNode> Body, int Line) : ASTNode;
public record IfNode(ExpressionNode Condition, List<ASTNode> ThenBranch, List<ASTNode> ElseBranch, int Line) : ASTNode;
public record ForNode(string VarName, ExpressionNode Iterable, List<ASTNode> Body, int Line) : ASTNode;
public record WhileNode(ExpressionNode Condition, List<ASTNode> Body, int Line) : ASTNode;
public record ReturnNode(ExpressionNode? Value, int Line) : ASTNode;
public record RunNode(ExpressionNode ScriptName, List<ExpressionNode> Args, int Line) : ASTNode;
public record ExpressionStmtNode(ExpressionNode Expr, int Line) : ASTNode;

public abstract record ExpressionNode;
public record LiteralExpr(NexusValue Value) : ExpressionNode;
public record VariableExpr(string Name) : ExpressionNode;
public record BinaryExpr(ExpressionNode Left, Operator Op, ExpressionNode Right) : ExpressionNode;
public record UnaryExpr(Operator Op, ExpressionNode Right) : ExpressionNode;
public record CallExpr(ExpressionNode Callee, List<ExpressionNode> Args, int Line) : ExpressionNode;
public record GetAttrExpr(ExpressionNode Obj, string Attr) : ExpressionNode;
public record NewObjectExpr(string ClassName, List<ExpressionNode> Args, int Line) : ExpressionNode;

internal class Lexer
{
    private static readonly Dictionary<string, Keyword> Keywords = new()
    {
        ["func"] = Keyword.FUNC, ["set"] = Keyword.SET, ["if"] = Keyword.IF,
        ["else"] = Keyword.ELSE, ["for"] = Keyword.FOR, ["while"] = Keyword.WHILE,
        ["return"] = Keyword.RETURN, ["new"] = Keyword.NEW, ["run"] = Keyword.RUN,
        ["true"] = Keyword.TRUE, ["false"] = Keyword.FALSE,
        ["break"] = Keyword.BREAK, ["continue"] = Keyword.CONTINUE
    };

    private readonly string _source;
    private int _start;
    private int _current;
    private int _lineNumber = 1;
    private int _columnNumber = 1;
    private readonly List<Token> _tokens = new();

    public Lexer(string source) { _source = source; }

    public List<Token> Tokenize()
    {
        while (!IsAtEnd())
        {
            _start = _current;
            ScanToken();
        }
        _tokens.Add(new PunctuationToken("", Punctuation.SEMICOLON, _lineNumber, _columnNumber));
        return _tokens;
    }

    private void ScanToken()
    {
        var c = Advance();
        switch (c)
        {
            case '(': AddPunct(Punctuation.LPAREN); break;
            case ')': AddPunct(Punctuation.RPAREN); break;
            case '{': AddPunct(Punctuation.LBRACE); break;
            case '}': AddPunct(Punctuation.RBRACE); break;
            case '[': AddPunct(Punctuation.LBRACKET); break;
            case ']': AddPunct(Punctuation.RBRACKET); break;
            case ',': AddPunct(Punctuation.COMMA); break;
            case ';': AddPunct(Punctuation.SEMICOLON); break;
            case ':':
                if (Match('>')) AddPunct(Punctuation.ARROW);
                else AddPunct(Punctuation.COLON);
                break;
            case '+': AddOp(Operator.PLUS); break;
            case '-':
                if (Match('>')) AddPunct(Punctuation.ARROW);
                else AddOp(Operator.MINUS);
                break;
            case '*': AddOp(Operator.MULT); break;
            case '/':
                if (Match('/'))
                {
                    while (Peek() != '\n' && !IsAtEnd()) Advance();
                }
                else AddOp(Operator.DIV);
                break;
            case '!': AddOp(Match('=') ? Operator.NEQ : Operator.NOT); break;
            case '=': AddOp(Match('=') ? Operator.EQ : Operator.ASSIGN); break;
            case '<': AddOp(Match('=') ? Operator.LTE : Operator.LT); break;
            case '>': AddOp(Match('=') ? Operator.GTE : Operator.GT); break;
            case '&':
                if (Match('&')) AddOp(Operator.AND);
                else Error("Expected '&&'");
                break;
            case '|':
                if (Match('|')) AddOp(Operator.OR);
                else Error("Expected '||'");
                break;
            case '.': AddOp(Operator.DOT); break;
            case '"': ScanString(); break;
            case ' ':
            case '\t':
            case '\r': break;
            case '\n': _lineNumber++; _columnNumber = 1; break;
            default:
                if (char.IsDigit(c)) ScanNumber();
                else if (char.IsLetter(c) || c == '_') ScanIdentifier();
                else Error($"Unexpected character '{c}'");
                break;
        }
    }

    private void ScanString()
    {
        var sb = new StringBuilder();
        while (Peek() != '"' && !IsAtEnd())
        {
            if (Peek() == '\n') _lineNumber++;
            sb.Append(Advance());
        }
        if (IsAtEnd()) Error("Unterminated string");
        Advance();
        _tokens.Add(new StringToken(sb.ToString(), sb.ToString(), _lineNumber, _columnNumber));
    }

    private void ScanNumber()
    {
        var sb = new StringBuilder();
        while (Peek() is not null && char.IsDigit(Peek()!.Value)) sb.Append(Advance());
        if (Peek() == '.' && Peek(1) is not null && char.IsDigit(Peek(1)!.Value))
        {
            sb.Append(Advance());
            while (Peek() is not null && char.IsDigit(Peek()!.Value)) sb.Append(Advance());
        }
        _tokens.Add(new NumberToken(sb.ToString(), double.Parse(sb.ToString()), _lineNumber, _columnNumber));
    }

    private void ScanIdentifier()
    {
        var sb = new StringBuilder();
        while (Peek() is not null && (char.IsLetterOrDigit(Peek()!.Value) || Peek() == '_')) sb.Append(Advance());
        var word = sb.ToString();
        if (Keywords.TryGetValue(word, out var keyword))
            _tokens.Add(new KeywordToken(word, keyword, _lineNumber, _columnNumber));
        else
            _tokens.Add(new IdentifierToken(word, _lineNumber, _columnNumber));
    }

    private char Advance()
    {
        _columnNumber++;
        return _source[_current++];
    }

    private char? Peek(int offset = 0) =>
        _current + offset < _source.Length ? _source[_current + offset] : null;

    private bool Match(char expected)
    {
        if (IsAtEnd() || _source[_current] != expected) return false;
        _current++;
        _columnNumber++;
        return true;
    }

    private bool IsAtEnd() => _current >= _source.Length;
    private void AddPunct(Punctuation p) => _tokens.Add(new PunctuationToken(GetPunctSym(p), p, _lineNumber, _columnNumber));
    private void AddOp(Operator o) => _tokens.Add(new OperatorToken(GetOpSym(o), o, _lineNumber, _columnNumber));
    private void Error(string msg) => throw new ScriptError(msg, _lineNumber, _columnNumber);

    private static string GetPunctSym(Punctuation p) => p switch
    {
        Punctuation.LPAREN => "(", Punctuation.RPAREN => ")",
        Punctuation.LBRACE => "{", Punctuation.RBRACE => "}",
        Punctuation.LBRACKET => "[", Punctuation.RBRACKET => "]",
        Punctuation.COMMA => ",", Punctuation.SEMICOLON => ";",
        Punctuation.COLON => ":", Punctuation.ARROW => "->",
        _ => ""
    };

    private static string GetOpSym(Operator o) => o switch
    {
        Operator.PLUS => "+", Operator.MINUS => "-", Operator.MULT => "*",
        Operator.DIV => "/", Operator.ASSIGN => "=", Operator.EQ => "==",
        Operator.NEQ => "!=", Operator.LT => "<", Operator.GT => ">",
        Operator.LTE => "<=", Operator.GTE => ">=", Operator.AND => "&&",
        Operator.OR => "||", Operator.NOT => "!", Operator.DOT => ".",
        _ => ""
    };
}

internal class Parser
{
    private readonly List<Token> _tokens;
    private int _current;

    public Parser(List<Token> tokens) { _tokens = tokens; }

    public ProgramNode Parse()
    {
        var statements = new List<ASTNode>();
        while (!IsAtEnd())
        {
            var stmt = Declaration();
            if (stmt is not null) statements.Add(stmt);
        }
        return new ProgramNode(statements);
    }

    private ASTNode? Declaration()
    {
        try
        {
            if (Match(Keyword.FUNC)) return FuncDecl();
            if (Match(Keyword.SET)) return VarDecl();
            return Statement();
        }
        catch (ScriptError)
        {
            Synchronize();
            return null;
        }
    }

    private ASTNode FuncDecl()
    {
        var line = Previous().Line;
        var name = Consume(TokenType.IDENTIFIER, "Expected function name").Lexeme;
        Consume(TokenType.PUNCTUATION, "Expected '('",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LPAREN);
        var parameters = new List<string>();
        if (!(Check(TokenType.PUNCTUATION) && Peek() is PunctuationToken pt2 && pt2.Punct == Punctuation.RPAREN))
        {
            do
            {
                parameters.Add(Consume(TokenType.IDENTIFIER, "Expected parameter name").Lexeme);
            } while (Match(Punctuation.COMMA));
        }
        Consume(TokenType.PUNCTUATION, "Expected ')'",
            t => t is PunctuationToken pt3 && pt3.Punct == Punctuation.RPAREN);
        Consume(TokenType.PUNCTUATION, "Expected '{'",
            t => t is PunctuationToken pt4 && pt4.Punct == Punctuation.LBRACE);
        var body = new List<ASTNode>();
        while (!(Check(TokenType.PUNCTUATION) && Peek() is PunctuationToken pt5 && pt5.Punct == Punctuation.RBRACE))
        {
            if (IsAtEnd()) throw Error(Peek(), "Unterminated function body");
            var d = Declaration();
            if (d is not null) body.Add(d);
        }
        Consume(TokenType.PUNCTUATION, "Expected '}'",
            t => t is PunctuationToken pt6 && pt6.Punct == Punctuation.RBRACE);
        return new FuncDeclNode(name, parameters, body, line);
    }

    private ASTNode VarDecl()
    {
        var line = Previous().Line;
        var name = Consume(TokenType.IDENTIFIER, "Expected variable name").Lexeme;
        Consume(TokenType.OPERATOR, "Expected '='",
            t => t is OperatorToken ot && ot.Op == Operator.ASSIGN);
        var value = Expression();
        Consume(TokenType.PUNCTUATION, "Expected ';'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.SEMICOLON);
        return new VarDeclNode(name, value, line);
    }

    private ASTNode Statement()
    {
        var line = Peek().Line;
        if (Match(Keyword.IF)) return IfStmt();
        if (Match(Keyword.FOR)) return ForStmt();
        if (Match(Keyword.WHILE)) return WhileStmt();
        if (Match(Keyword.RETURN)) return ReturnStmt();
        if (Match(Keyword.RUN)) return RunStmt();
        var expr = Expression();
        Consume(TokenType.PUNCTUATION, "Expected ';'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.SEMICOLON);
        return new ExpressionStmtNode(expr, line);
    }

    private ASTNode IfStmt()
    {
        var line = Previous().Line;
        Consume(TokenType.PUNCTUATION, "Expected '('",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LPAREN);
        var condition = Expression();
        Consume(TokenType.PUNCTUATION, "Expected ')'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.RPAREN);
        Consume(TokenType.PUNCTUATION, "Expected '{'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LBRACE);
        var thenBranch = Block();
        var elseBranch = Match(Keyword.ELSE)
            ? (Consume(TokenType.PUNCTUATION, "Expected '{'",
                t => t is PunctuationToken pt && pt.Punct == Punctuation.LBRACE), Block()).Item2
            : new List<ASTNode>();
        return new IfNode(condition, thenBranch, elseBranch, line);
    }

    private ASTNode ForStmt()
    {
        var line = Previous().Line;
        Consume(TokenType.PUNCTUATION, "Expected '('",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LPAREN);
        var varName = Consume(TokenType.IDENTIFIER, "Expected variable name").Lexeme;
        Consume(TokenType.KEYWORD, "Expected 'in'",
            t => t is KeywordToken kt && kt.Keyword == Keyword.SET);
        var iterable = Expression();
        Consume(TokenType.PUNCTUATION, "Expected ')'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.RPAREN);
        Consume(TokenType.PUNCTUATION, "Expected '{'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LBRACE);
        var body = Block();
        return new ForNode(varName, iterable, body, line);
    }

    private ASTNode WhileStmt()
    {
        var line = Previous().Line;
        Consume(TokenType.PUNCTUATION, "Expected '('",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LPAREN);
        var condition = Expression();
        Consume(TokenType.PUNCTUATION, "Expected ')'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.RPAREN);
        Consume(TokenType.PUNCTUATION, "Expected '{'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LBRACE);
        var body = Block();
        return new WhileNode(condition, body, line);
    }

    private ASTNode ReturnStmt()
    {
        var line = Previous().Line;
        ExpressionNode? value = null;
        if (!(Check(TokenType.PUNCTUATION) && Peek() is PunctuationToken pt && pt.Punct == Punctuation.SEMICOLON))
        {
            value = Expression();
        }
        Consume(TokenType.PUNCTUATION, "Expected ';'",
            t => t is PunctuationToken pt2 && pt2.Punct == Punctuation.SEMICOLON);
        return new ReturnNode(value, line);
    }

    private ASTNode RunStmt()
    {
        var line = Previous().Line;
        var scriptName = Expression();
        Consume(TokenType.PUNCTUATION, "Expected ';'",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.SEMICOLON);
        return new RunNode(scriptName, new List<ExpressionNode>(), line);
    }

    private List<ASTNode> Block()
    {
        var stmts = new List<ASTNode>();
        while (!(Check(TokenType.PUNCTUATION) && Peek() is PunctuationToken pt && pt.Punct == Punctuation.RBRACE))
        {
            if (IsAtEnd()) throw Error(Peek(), "Unterminated block");
            var d = Declaration();
            if (d is not null) stmts.Add(d);
        }
        Consume(TokenType.PUNCTUATION, "Expected '}'",
            t => t is PunctuationToken pt2 && pt2.Punct == Punctuation.RBRACE);
        return stmts;
    }

    private ExpressionNode Expression() => Assignment();
    private ExpressionNode Assignment()
    {
        var expr = OrExpr();
        if (Match(Operator.ASSIGN))
        {
            var value = Assignment();
            if (expr is VariableExpr ve)
                expr = new BinaryExpr(ve, Operator.ASSIGN, value);
            else
                throw Error(Peek(), "Invalid assignment target");
        }
        return expr;
    }

    private ExpressionNode OrExpr()
    {
        var expr = AndExpr();
        while (Match(Operator.OR))
        {
            var op = (Previous() as OperatorToken)!.Op;
            var right = AndExpr();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private ExpressionNode AndExpr()
    {
        var expr = Equality();
        while (Match(Operator.AND))
        {
            var op = (Previous() as OperatorToken)!.Op;
            var right = Equality();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private ExpressionNode Equality()
    {
        var expr = Comparison();
        while (Match(Operator.EQ, Operator.NEQ))
        {
            var op = (Previous() as OperatorToken)!.Op;
            var right = Comparison();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private ExpressionNode Comparison()
    {
        var expr = Term();
        while (Match(Operator.LT, Operator.GT, Operator.LTE, Operator.GTE))
        {
            var op = (Previous() as OperatorToken)!.Op;
            var right = Term();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private ExpressionNode Term()
    {
        var expr = Factor();
        while (Match(Operator.PLUS, Operator.MINUS))
        {
            var op = (Previous() as OperatorToken)!.Op;
            var right = Factor();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private ExpressionNode Factor()
    {
        var expr = Unary();
        while (Match(Operator.MULT, Operator.DIV))
        {
            var op = (Previous() as OperatorToken)!.Op;
            var right = Unary();
            expr = new BinaryExpr(expr, op, right);
        }
        return expr;
    }

    private ExpressionNode Unary()
    {
        if (Match(Operator.MINUS, Operator.NOT))
            return new UnaryExpr((Previous() as OperatorToken)!.Op, Unary());
        return Call();
    }

    private ExpressionNode Call()
    {
        var expr = Primary();
        while (true)
        {
            if (Match(Punctuation.LPAREN))
            {
                var args = ArgumentList();
                Consume(TokenType.PUNCTUATION, "Expected ')'",
                    t => t is PunctuationToken pt && pt.Punct == Punctuation.RPAREN);
                expr = new CallExpr(expr, args, Peek().Line);
            }
            else if (Match(Operator.DOT))
            {
                var name = Consume(TokenType.IDENTIFIER, "Expected property name").Lexeme;
                expr = new GetAttrExpr(expr, name);
            }
            else break;
        }
        return expr;
    }

    private ExpressionNode Primary()
    {
        if (Match(Keyword.TRUE)) return new LiteralExpr(NexusValue.BoolVal(true));
        if (Match(Keyword.FALSE)) return new LiteralExpr(NexusValue.BoolVal(false));
        if (Match(Keyword.NEW)) return NewObjectExpr();
        if (Match(Punctuation.LBRACKET))
        {
            var elements = new List<ExpressionNode>();
            if (!(Check(TokenType.PUNCTUATION) && Peek() is PunctuationToken pt && pt.Punct == Punctuation.RBRACKET))
            {
                do { elements.Add(Expression()); } while (Match(Punctuation.COMMA));
            }
            Consume(TokenType.PUNCTUATION, "Expected ']'",
                t => t is PunctuationToken pt2 && pt2.Punct == Punctuation.RBRACKET);
            return new CallExpr(new VariableExpr("array"), elements, Peek().Line);
        }
        if (Match(Punctuation.LPAREN))
        {
            var expr = Expression();
            Consume(TokenType.PUNCTUATION, "Expected ')'",
                t => t is PunctuationToken pt && pt.Punct == Punctuation.RPAREN);
            return expr;
        }
        var token = Advance();
        return token switch
        {
            NumberToken nt => new LiteralExpr(NexusValue.NumVal(nt.Value)),
            StringToken st => new LiteralExpr(NexusValue.StrVal(st.Value)),
            IdentifierToken id => new VariableExpr(id.Name),
            _ => throw Error(token, "Expected expression")
        };
    }

    private ExpressionNode NewObjectExpr()
    {
        var className = Consume(TokenType.IDENTIFIER, "Expected class name").Lexeme;
        Consume(TokenType.PUNCTUATION, "Expected '('",
            t => t is PunctuationToken pt && pt.Punct == Punctuation.LPAREN);
        var args = ArgumentList();
        Consume(TokenType.PUNCTUATION, "Expected ')'",
            t => t is PunctuationToken pt2 && pt2.Punct == Punctuation.RPAREN);
        return new NewObjectExpr(className, args, Peek().Line);
    }

    private List<ExpressionNode> ArgumentList()
    {
        var args = new List<ExpressionNode>();
        if (!(Check(TokenType.PUNCTUATION) && Peek() is PunctuationToken pt && pt.Punct == Punctuation.RPAREN))
        {
            do { args.Add(Expression()); } while (Match(Punctuation.COMMA));
        }
        return args;
    }

    private Token Advance()
    {
        if (!IsAtEnd()) _current++;
        return Previous();
    }

    private Token Previous() => _tokens[_current - 1];
    private Token Peek() => _tokens[_current];

    private bool IsAtEnd()
    {
        if (_current >= _tokens.Count) return true;
        var token = _tokens[_current];
        return token is PunctuationToken pt && pt.Punct == Punctuation.SEMICOLON && _current >= _tokens.Count - 1;
    }

    private bool Check(TokenType type) => !IsAtEnd() && Peek().Type == type;

    private bool Match(Keyword keyword)
    {
        if (IsAtEnd() || Peek() is not KeywordToken kt) return false;
        if (kt.Keyword == keyword) { Advance(); return true; }
        return false;
    }

    private bool Match(Punctuation punct)
    {
        if (IsAtEnd() || Peek() is not PunctuationToken pt) return false;
        if (pt.Punct == punct) { Advance(); return true; }
        return false;
    }

    private bool Match(params Operator[] ops)
    {
        if (IsAtEnd() || Peek() is not OperatorToken ot) return false;
        if (ops.Contains(ot.Op)) { Advance(); return true; }
        return false;
    }

    private Token Consume(TokenType type, string message, Func<Token, bool>? predicate = null)
    {
        if (Check(type) && (predicate is null || predicate(Peek()))) return Advance();
        throw Error(Peek(), message);
    }

    private ScriptError Error(Token token, string msg) => new(msg, token.Line, 0);

    private void Synchronize()
    {
        while (!IsAtEnd())
        {
            if (Previous() is PunctuationToken pt && pt.Punct == Punctuation.SEMICOLON) return;
            Advance();
        }
    }
}

internal class Interpreter
{
    private readonly ScriptContext _context;
    private readonly IReadOnlyDictionary<string, GameCommand>? _builtinCommands;
    public List<string> Console { get; } = new();

    public Interpreter(ScriptContext context,
        IReadOnlyDictionary<string, GameCommand>? builtinCommands = null)
    {
        _context = context;
        _builtinCommands = builtinCommands;
    }

    public (string, NexusValue) Interpret(ASTNode node)
    {
        switch (node)
        {
            case ProgramNode prog:
                foreach (var stmt in prog.Statements)
                    ExecuteStmt(stmt);
                return (_context.Output.ToString(), _context.ReturnValue);
            default:
                ExecuteStmt(node);
                return (_context.Output.ToString(), _context.ReturnValue);
        }
    }

    private void ExecuteStmt(ASTNode node)
    {
        if (_context.OpBudget <= 0)
            throw new ScriptError("Operation budget exceeded", 0, 0);
        _context.OpBudget--;

        if (_context.NestingDepth > ScriptContext.MaxNesting)
            throw new ScriptError("Maximum nesting depth exceeded", 0, 0);

        switch (node)
        {
            case ProgramNode prog:
                foreach (var stmt in prog.Statements)
                    ExecuteStmt(stmt);
                break;
            case VarDeclNode vd:
                _context.Variables[vd.Name] = Evaluate(vd.Value);
                break;
            case FuncDeclNode fd:
                _context.Functions[fd.Name] = fd;
                break;
            case IfNode ifn:
            {
                var cond = Evaluate(ifn.Condition);
                if (cond.IsTruthy())
                {
                    _context.NestingDepth++;
                    foreach (var stmt in ifn.ThenBranch)
                        ExecuteStmt(stmt);
                    _context.NestingDepth--;
                }
                else
                {
                    _context.NestingDepth++;
                    foreach (var stmt in ifn.ElseBranch)
                        ExecuteStmt(stmt);
                    _context.NestingDepth--;
                }
                break;
            }
            case ForNode forn:
            {
                var iterable = Evaluate(forn.Iterable);
                var elements = iterable switch
                {
                    NexusValue.ArrayValue av => av.Elements,
                    NexusValue.StrValue sv => sv.Value.Select(c => NexusValue.StrVal(c.ToString())).ToList(),
                    _ => new List<NexusValue>()
                };
                foreach (var element in elements)
                {
                    _context.Variables[forn.VarName] = element;
                    _context.NestingDepth++;
                    foreach (var stmt in forn.Body)
                        ExecuteStmt(stmt);
                    _context.NestingDepth--;
                }
                break;
            }
            case WhileNode whilen:
            {
                while (Evaluate(whilen.Condition).IsTruthy())
                {
                    if (_context.OpBudget <= 0)
                        throw new ScriptError("Operation budget exceeded in while loop", 0, 0);
                    _context.NestingDepth++;
                    foreach (var stmt in whilen.Body)
                        ExecuteStmt(stmt);
                    _context.NestingDepth--;
                }
                break;
            }
            case ReturnNode rn:
                _context.ReturnValue = rn.Value is not null ? Evaluate(rn.Value) : NexusValue.NullVal();
                break;
            case RunNode runn:
            {
                var scriptName = Evaluate(runn.ScriptName).AsString();
                Console.Add($"[RUN] Attempting to execute script: {scriptName}");
                _context.Output.AppendLine($"[RUN] Script '{scriptName}' executed (simulated)");
                break;
            }
            case ExpressionStmtNode esn:
                Evaluate(esn.Expr);
                break;
        }
    }

    private NexusValue Evaluate(ExpressionNode expr)
    {
        if (_context.OpBudget <= 0)
            throw new ScriptError("Operation budget exceeded", 0, 0);
        _context.OpBudget--;

        return expr switch
        {
            LiteralExpr le => le.Value,
            VariableExpr ve => EvaluateVariable(ve),
            BinaryExpr be => EvaluateBinary(be),
            UnaryExpr ue => EvaluateUnary(ue),
            CallExpr ce => EvaluateCall(ce),
            GetAttrExpr gae => EvaluateGetAttr(gae),
            NewObjectExpr noe => EvaluateNewObject(noe),
            _ => NexusValue.NullVal()
        };
    }

    private NexusValue EvaluateVariable(VariableExpr expr)
    {
        var name = expr.Name;
        if (_context.Variables.ContainsKey(name))
            return _context.Variables[name];
        if (_context.Functions.ContainsKey(name))
            return NexusValue.StrVal($"[Function: {name}]");
        _context.Variables[name] = NexusValue.NullVal();
        return NexusValue.NullVal();
    }

    private NexusValue EvaluateUnary(UnaryExpr expr)
    {
        var right = Evaluate(expr.Right);
        return expr.Op switch
        {
            Operator.MINUS => NexusValue.NumVal(-right.AsDouble()),
            Operator.NOT => NexusValue.BoolVal(!right.IsTruthy()),
            _ => NexusValue.NullVal()
        };
    }

    private NexusValue EvaluateBinary(BinaryExpr expr)
    {
        switch (expr.Op)
        {
            case Operator.ASSIGN:
                if (expr.Left is VariableExpr ve)
                {
                    var value = Evaluate(expr.Right);
                    _context.Variables[ve.Name] = value;
                    return value;
                }
                return NexusValue.NullVal();

            case Operator.PLUS:
            case Operator.MINUS:
            case Operator.MULT:
            case Operator.DIV:
            {
                var left = Evaluate(expr.Left).AsDouble();
                var right = Evaluate(expr.Right).AsDouble();
                var result = expr.Op switch
                {
                    Operator.PLUS => left + right,
                    Operator.MINUS => left - right,
                    Operator.MULT => left * right,
                    Operator.DIV => right != 0.0 ? left / right : 0.0,
                    _ => 0.0
                };
                return NexusValue.NumVal(result);
            }

            case Operator.EQ:
                return NexusValue.BoolVal(Evaluate(expr.Left).AsString() == Evaluate(expr.Right).AsString());
            case Operator.NEQ:
                return NexusValue.BoolVal(Evaluate(expr.Left).AsString() != Evaluate(expr.Right).AsString());
            case Operator.LT:
                return NexusValue.BoolVal(Evaluate(expr.Left).AsDouble() < Evaluate(expr.Right).AsDouble());
            case Operator.GT:
                return NexusValue.BoolVal(Evaluate(expr.Left).AsDouble() > Evaluate(expr.Right).AsDouble());
            case Operator.LTE:
                return NexusValue.BoolVal(Evaluate(expr.Left).AsDouble() <= Evaluate(expr.Right).AsDouble());
            case Operator.GTE:
                return NexusValue.BoolVal(Evaluate(expr.Left).AsDouble() >= Evaluate(expr.Right).AsDouble());
            case Operator.AND:
                return NexusValue.BoolVal(Evaluate(expr.Left).IsTruthy() && Evaluate(expr.Right).IsTruthy());
            case Operator.OR:
                return NexusValue.BoolVal(Evaluate(expr.Left).IsTruthy() || Evaluate(expr.Right).IsTruthy());
            default:
                return NexusValue.NullVal();
        }
    }

    private NexusValue EvaluateCall(CallExpr expr)
    {
        var args = expr.Args.Select(Evaluate).ToList();

        if (expr.Callee is VariableExpr ve)
        {
            var name = ve.Name;

            if (_context.Functions.TryGetValue(name, out var func))
            {
                var oldVars = new Dictionary<string, NexusValue>(_context.Variables);
                for (int i = 0; i < func.Params.Count; i++)
                    _context.Variables[func.Params[i]] = i < args.Count ? args[i] : NexusValue.NullVal();
                foreach (var stmt in func.Body)
                    ExecuteStmt(stmt);
                var ret = _context.ReturnValue;
                _context.ReturnValue = NexusValue.NullVal();
                _context.Variables.Clear();
                foreach (var kv in oldVars)
                    _context.Variables[kv.Key] = kv.Value;
                return ret;
            }

            switch (name)
            {
                case "print":
                case "printline":
                {
                    var msg = string.Join(", ", args.Select(a => a.AsString()));
                    Console.Add(msg);
                    _context.Output.AppendLine(msg);
                    return NexusValue.NullVal();
                }
                case "sleep":
                    return NexusValue.NullVal();
                case "scan":
                {
                    var target = args.Count > 0 ? args[0].AsString() : "localhost";
                    _context.Output.AppendLine($"[SCAN] Scanning target: {target}");
                    _context.Output.AppendLine("Host appears to be online. 3 ports discovered.");
                    return NexusValue.StrVal($"Scan of {target} complete");
                }
                case "connect":
                {
                    var target = args.Count > 0 ? args[0].AsString() : "";
                    _context.Output.AppendLine($"[CONNECT] Establishing connection to {target}...");
                    _context.Output.AppendLine($"Connected to {target}");
                    return NexusValue.StrVal($"Connected to {target}");
                }
                case "exploit":
                {
                    var target = args.Count > 0 ? args[0].AsString() : "unknown";
                    var payload = args.Count > 1 ? args[1].AsString() : "default";
                    _context.Output.AppendLine($"[EXPLOIT] Targeting {target} with {payload}");
                    _context.Output.AppendLine("Exploit delivered. Target compromised.");
                    return NexusValue.StrVal($"Exploited {target}");
                }
            }

            if (_builtinCommands is not null && _builtinCommands.TryGetValue(name, out var cmd))
            {
                _context.Output.AppendLine($"[{name.ToUpper()}] {cmd.Description}");
                _context.Output.AppendLine($"[Syntax: {cmd.Syntax}]");
                _context.Output.AppendLine($"[Cost: {cmd.CpuCost} CPU / {cmd.RamCost} RAM]");
                return NexusValue.StrVal($"[Executed: {name}]");
            }

            return NexusValue.NullVal();
        }

        if (expr.Callee is GetAttrExpr gae)
        {
            var obj = Evaluate(gae.Obj);
            if (obj is NexusValue.ObjValue ov)
                return ov.Obj.Call(gae.Attr, args);
            return NexusValue.NullVal();
        }

        return NexusValue.NullVal();
    }

    private NexusValue EvaluateGetAttr(GetAttrExpr expr)
    {
        var obj = Evaluate(expr.Obj);
        return obj is NexusValue.ObjValue ov
            ? ov.Obj.Get(expr.Attr)
            : NexusValue.NullVal();
    }

    private NexusValue EvaluateNewObject(NewObjectExpr expr)
    {
        var args = expr.Args.Select(Evaluate).ToList();
        NexusObject obj = expr.ClassName switch
        {
            "IP_Object" => new IPObject(args.Count > 0 ? args[0].AsString() : "0.0.0.0"),
            "Port_Object" => new PortObject(
                args.Count > 0 ? (int)args[0].AsDouble() : 0,
                args.Count > 1 ? args[1].AsString() : "tcp"
            ),
            "Exploit_Object" => new ExploitObject(
                args.Count > 0 ? args[0].AsString() : "generic_exploit",
                args.Count > 1 ? (int)args[1].AsDouble() : 5
            ),
            _ => new NexusObject(expr.ClassName)
        };
        return NexusValue.ObjRef(obj);
    }
}

public class NexusScriptEngine
{
    private readonly IReadOnlyDictionary<string, GameCommand>? _builtinCommands;

    public NexusScriptEngine(IReadOnlyDictionary<string, GameCommand>? builtinCommands = null)
    {
        _builtinCommands = builtinCommands;
    }

    public ScriptResult Execute(NexusScript script, ScriptContext context)
    {
        try
        {
            var tokens = new Lexer(script.Code).Tokenize();
            var ast = new Parser(tokens).Parse();
            var interpreter = new Interpreter(context, _builtinCommands);
            var result = interpreter.Interpret(ast);
            return new ScriptResult(
                Success: true,
                Output: result.Item1,
                ReturnValue: result.Item2,
                Console: string.Join("\n", interpreter.Console),
                Variables: context.Variables.ToDictionary(kv => kv.Key, kv => kv.Value.AsString()),
                ExecutionTimeMs: 0
            );
        }
        catch (ScriptError e)
        {
            return new ScriptResult(
                Success: false,
                Error: e.Message,
                Console: "",
                ExecutionTimeMs: 0
            );
        }
        catch (Exception e)
        {
            return new ScriptResult(
                Success: false,
                Error: $"Runtime error: {e.Message}",
                Console: "",
                ExecutionTimeMs: 0
            );
        }
    }

    public ValidationResult Validate(string code)
    {
        try
        {
            var tokens = new Lexer(code).Tokenize();
            new Parser(tokens).Parse();
            return new ValidationResult(Valid: true);
        }
        catch (ScriptError e)
        {
            return new ValidationResult(Valid: false, Errors: new List<string> { e.Message });
        }
        catch (Exception e)
        {
            return new ValidationResult(Valid: false, Errors: new List<string> { $"Validation error: {e.Message}" });
        }
    }

    public record ScriptResult(
        bool Success,
        string Output = "",
        NexusValue? ReturnValue = null,
        string Console = "",
        Dictionary<string, string>? Variables = null,
        string Error = "",
        long ExecutionTimeMs = 0
    );

    public record ValidationResult(
        bool Valid,
        List<string>? Errors = null
    );
}
