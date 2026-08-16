# Task Tracker CLI — Guia Completo do Projeto

[Voltar ao README](../README.md) ·
[Read in English](PROJECT_GUIDE.md)

## 1. Objetivo deste guia

Este documento explica como o Task Tracker CLI foi construído, por que cada
camada existe, como os dados percorrem a aplicação, o que cada etapa ensina e
quais bibliotecas normalmente substituem o código manual em projetos reais.

O projeto evita dependências externas de propósito. Por isso, algumas partes são
mais trabalhosas do que seriam em uma aplicação Java profissional, principalmente
a conversão de JSON, o parsing dos comandos, os testes e o empacotamento. A ideia
é enxergar os mecanismos que bibliotecas e frameworks geralmente escondem.

## 2. Objetivo e restrições do projeto

A aplicação implementa o desafio
[Task Tracker do roadmap.sh](https://roadmap.sh/projects/task-tracker). Ela
permite:

- Criar tarefas.
- Atualizar descrições.
- Apagar tarefas.
- Marcar tarefas como `in-progress` ou `done`.
- Listar todas as tarefas.
- Filtrar por `todo`, `in-progress` ou `done`.
- Persistir os dados em um arquivo JSON no diretório atual.

O projeto utiliza:

- Java 17.
- Argumentos posicionais na linha de comando.
- Apenas a biblioteca padrão do Java.
- Um arquivo local chamado `tasks.json`.
- Assertions nativas do Java nos testes.
- As ferramentas `javac`, `java` e `jar` do próprio JDK.

## 3. Como um comando percorre a aplicação

Para o comando:

```bash
java -cp out tasktracker.Main add "Estudar Java"
```

o fluxo é:

```text
Sistema operacional
└── Main.main(args)
    └── TaskCli.run(args)
        ├── JsonTaskRepository.initialize()
        ├── JsonTaskRepository.loadTasks()
        ├── valida e direciona o comando "add"
        ├── cria uma Task
        ├── JsonTaskRepository.saveTasks()
        └── retorna o código de saída
```

Cada execução no terminal inicia uma nova JVM. A memória da execução anterior não
existe mais, então `TaskCli` recarrega o JSON antes de processar qualquer comando.
Depois de uma alteração, a lista completa é gravada novamente.

## 4. Arquitetura e responsabilidades

### `Main`: fronteira da aplicação

`Main` cria as dependências concretas:

```java
JsonTaskRepository repository =
        new JsonTaskRepository(Path.of("tasks.json"));
TaskCli cli = new TaskCli(repository);
```

Depois, ela repassa ao sistema operacional o código retornado por `TaskCli`.
`System.exit` fica nessa camada externa para que a lógica interna possa ser
testada sem encerrar a JVM dos testes.

### `TaskCli`: orquestração dos comandos

`TaskCli` é responsável por:

- Carregar as tarefas persistidas.
- Validar a quantidade de argumentos.
- Converter IDs positivos.
- Transformar o texto de status em `TaskStatus`.
- Direcionar comandos para seus handlers.
- Encontrar tarefas pelo ID.
- Coordenar alterações no modelo e a persistência.
- Formatar a saída do terminal.
- Transformar falhas em códigos de saída do processo.

Ela não conhece os detalhes da sintaxe JSON. Essa responsabilidade pertence ao
repositório.

### `Task`: entidade de domínio

`Task` controla o estado e as regras de uma tarefa:

- `id` deve ser positivo e não pode mudar.
- `description` não pode ser nula, vazia ou composta apenas por espaços.
- Uma nova tarefa começa com status `todo`.
- `status` não pode ser nulo.
- `createdAt` não pode mudar.
- Alterar descrição ou status atualiza `updatedAt`.
- Uma tarefa restaurada não pode ter `updatedAt` anterior a `createdAt`.

Existem dois cenários de construção:

1. Uma tarefa nova recebe ID e descrição. O modelo define o status inicial e os
   timestamps atuais.
2. Uma tarefa persistida é restaurada com suas cinco propriedades.

Essa separação impede que a leitura do JSON substitua status e datas persistidos
por valores novos.

### `TaskStatus`: conjunto de estados válidos

`TaskStatus` é um enum com:

- `TODO("todo")`
- `IN_PROGRESS("in-progress")`
- `DONE("done")`

O enum é mais seguro que uma `String` livre: estados inválidos não podem existir
dentro de uma `Task` válida. O método `fromValue` converte o texto recebido nas
fronteiras da CLI e do armazenamento.

### `JsonTaskRepository`: fronteira de persistência

O repositório cuida de:

- Criar o arquivo.
- Serializar tarefas.
- Escapar caracteres de strings JSON.
- Ler e desserializar tarefas.
- Validar toda a estrutura armazenada.
- Rejeitar IDs duplicados.
- Preservar status e timestamps.
- Substituir o arquivo com segurança.

Manter esses detalhes fora de `TaskCli` deixa o comportamento dos comandos mais
fácil de ler e testar.

## 5. Modelo dos dados

O arquivo contém um array JSON:

```json
[
  {
    "id": 1,
    "description": "Estudar Java",
    "status": "todo",
    "createdAt": "2026-08-16T12:00:00Z",
    "updatedAt": "2026-08-16T12:00:00Z"
  }
]
```

### Por que usar `Instant`?

`java.time.Instant` representa um momento inequívoco em UTC e gera um valor
ISO-8601 apropriado para persistência. Isso evita textos dependentes de região,
como `16/08/2026 09:00`, que podem ser interpretados de formas diferentes.

### Geração dos IDs

Antes de adicionar uma tarefa, a CLI encontra o maior ID persistido e usa o
próximo inteiro. `Math.addExact` detecta overflow:

```text
maior ID -> soma exata de 1 -> novo ID ou erro explícito
```

A estratégia atual garante unicidade entre as tarefas existentes. Uma
consequência é que apagar a tarefa de maior ID permite reutilizar esse ID depois.
Uma sequence de banco de dados normalmente manteria IDs crescentes e não os
reutilizaria.

## 6. Persistência JSON em detalhes

### Inicialização

Se `tasks.json` não existir, o repositório cria:

```json
[]
```

A inicialização nunca sobrescreve um arquivo existente.

### Serialização

O serializador usa `StringBuilder` para converter cada `Task` em um objeto JSON.
Ele coloca vírgulas entre objetos, mas não depois do último.

A descrição escapa:

- Barra invertida.
- Aspas duplas.
- Quebra de linha.
- Carriage return.
- Tabulação.

A barra invertida precisa ser escapada primeiro. Se fosse processada por último,
ela também escaparia as barras adicionadas pelas outras substituições.

### Desserialização

O repositório lê o arquivo completo e valida o array externo. Uma expressão
regular captura os cinco campos de cada objeto. Cada valor capturado é convertido:

- ID -> `int`
- Descrição -> `String` desescapada
- Status -> `TaskStatus`
- Timestamps -> `Instant`

O parser acompanha onde cada correspondência termina. O texto entre dois objetos
precisa ser exatamente uma vírgula e nenhum conteúdo desconhecido pode sobrar
depois do último objeto. Isso impede que `Matcher.find()` aceite silenciosamente
um objeto válido escondido dentro de conteúdo inválido.

Um `HashSet<Integer>` detecta IDs duplicados durante a leitura.

### Substituição segura do arquivo

Gravar diretamente em `tasks.json` poderia danificar os dados antigos se o
processo falhasse depois de truncar o arquivo. Por isso, o repositório:

1. Serializa a nova lista.
2. Grava em um arquivo temporário no mesmo diretório.
3. Tenta mover o temporário atomicamente sobre `tasks.json`.
4. Usa uma substituição comum se o filesystem não suportar movimento atômico.
5. Remove qualquer temporário restante em um bloco `finally`.

A exception informa que algo falhou; essa estratégia também reduz o risco de
perder o último arquivo válido.

### Limitações importantes do parser

Este é um parser educacional e específico para o schema da aplicação, não uma
implementação geral de JSON. Ele espera os campos produzidos pelo próprio
programa e na ordem conhecida. Ele não suporta qualquer ordem de campos,
propriedades desconhecidas, todas as formas válidas de JSON, todos os caracteres
de controle ou escapes Unicode.

Essas limitações são um dos principais motivos para aplicações reais usarem uma
biblioteca madura de JSON.

## 7. Comportamento dos comandos

### Adicionar

```bash
task-cli add "descrição"
```

A CLI valida a descrição, encontra o próximo ID, constrói uma nova `Task`,
adiciona à lista de trabalho e persiste a lista.

### Atualizar

```bash
task-cli update <id> "descrição"
```

A CLI converte o ID, encontra a tarefa, delega a mudança para
`Task.updateDescription` e persiste o resultado. O modelo atualiza
`updatedAt`.

### Apagar

```bash
task-cli delete <id>
```

A CLI encontra o objeto exato dentro da lista, remove-o e persiste as tarefas
restantes.

### Mudar o status

```bash
task-cli mark-in-progress <id>
task-cli mark-done <id>
```

Os dois handlers compartilham um único método de mudança de status. Isso evita
duplicar busca, alteração no modelo, persistência e saída.

### Listar e filtrar

```bash
task-cli list
task-cli list todo
task-cli list in-progress
task-cli list done
```

Sem filtro, todas as tarefas são exibidas. Com filtro, a identidade do enum é
usada para comparar status. Uma mensagem clara aparece quando nenhuma tarefa
corresponde.

## 8. Exceptions, tratamento e códigos de saída

Exceptions são o mecanismo de sinalização, não todo o tratamento de erros. A
aplicação também precisa decidir:

- Onde validar.
- Qual camada traduz o erro.
- O que o usuário deve ver.
- Como proteger os dados armazenados.
- Qual código o processo deve retornar.

`TaskCli.run` captura:

- `IllegalArgumentException` para comandos, argumentos, valores de domínio ou
  dados armazenados inválidos.
- `IOException` para falhas no filesystem.

Os códigos são:

| Código | Significado |
| --- | --- |
| `0` | Sucesso. |
| `1` | Comando, entrada, tarefa ou conteúdo JSON inválido. |
| `2` | Falha ao acessar o armazenamento. |

Retornar o código por `TaskCli`, em vez de chamar `System.exit` nessa classe,
permite testar a CLI diretamente.

## 9. As nove etapas de implementação

### Etapa 1 — Modelo de domínio

Foram criados `Task` e `TaskStatus`. O foco foi encapsulamento, invariantes,
imutabilidade da identidade e da data de criação e mudanças controladas de estado.

### Etapa 2 — Parsing dos comandos

Foi criada `TaskCli`, com direcionamento dos comandos, validação de quantidade de
argumentos, conversão de IDs e status, guia de uso e mensagens de erro. No início,
os handlers apenas confirmavam o parsing.

### Etapa 3 — Adição em memória

O comando `add` passou a criar uma `Task` real e adicioná-la a uma lista. Isso
expôs um problema: outra execução da CLI começa com memória vazia.

### Etapa 4 — Persistência JSON

Foi criado `JsonTaskRepository`, com inicialização, serialização,
desserialização, construtor de restauração, escaping e cálculo persistente de IDs.
O JSON substituiu a memória como fonte de verdade entre processos.

### Etapa 5 — Listagem e filtros

`list` passou a utilizar a coleção carregada e ganhou filtro opcional por enum.

### Etapa 6 — Atualização e exclusão

Foi criada a busca reutilizável por ID. Alterações de descrição foram delegadas ao
modelo e persistidas; a exclusão passou a remover a tarefa encontrada.

### Etapa 7 — Mudança de status

Os comandos de status foram ligados a `Task.updateStatus` e o fluxo compartilhado
foi extraído para evitar duplicação.

### Etapa 8 — Robustez

Foram adicionadas validação completa do conteúdo JSON, detecção de IDs duplicados,
substituição atômica, limpeza de temporários, proteção contra overflow, mensagens
para o usuário e códigos de saída.

### Etapa 9 — Testes e empacotamento

Os fontes de teste foram separados, testes de modelo, repositório e fluxo da CLI
foram adicionados, as assertions foram habilitadas e um JAR executável contendo
apenas produção foi criado.

## 10. Estratégia de testes

### Testes do modelo

`TaskTest` verifica:

- Valores iniciais de descrição, status e timestamps.
- Rejeição de descrição em branco.
- Rejeição de ID não positivo.

### Testes de integração do repositório

`JsonTaskRepositoryTest` usa diretórios temporários e verifica:

- Criação do armazenamento vazio.
- Ciclo de ida e volta pelo JSON.
- Preservação de caracteres especiais.
- Preservação de status e timestamps.

Os caminhos temporários isolam os testes do `tasks.json` real do usuário.

### Teste do fluxo da CLI

`TaskCliTest` executa:

```text
add -> add -> update -> mark done -> list filtrado -> delete
```

Ele também verifica o estado persistido e o código de saída de um status inválido.
A saída padrão é capturada quando o conteúdo da listagem precisa ser validado.

### Por que usar `-ea`?

Assertions Java ficam desabilitadas por padrão. Os testes precisam ser executados
com:

```bash
java -ea ...
```

Essa abordagem leve mantém a regra de não usar dependências. Em um projeto real,
normalmente usaríamos um framework de testes.

## 11. O que bibliotecas substituiriam em produção

| Implementação manual deste projeto | Substituição comum em produção | O que ela oferece |
| --- | --- | --- |
| Montagem de JSON, escaping, regex e conversão de tipos | Jackson, Gson ou JSON-B | Parsing geral, mapeamento, configuração e tratamento maduro de casos extremos. |
| `switch` de comandos e validação posicional | picocli, JCommander ou Spring Shell | Comandos declarativos, opções, help, conversão e exit codes. |
| Reescrita de um array JSON local | PostgreSQL/MySQL com JDBC, JPA/Hibernate ou Spring Data | Transações, concorrência, consultas, índices, constraints e IDs gerados. |
| Validações repetidas em construtores e campos | Jakarta Bean Validation / Hibernate Validator | Constraints declarativas, como `@NotBlank` e `@Positive`. |
| Test runners baseados em `assert` | JUnit 5, AssertJ e Mockito | Descoberta de testes, lifecycle, testes parametrizados, assertions expressivas e mocks. |
| Comandos manuais de `javac` e `jar` | Maven ou Gradle | Build reproduzível, dependências, testes, empacotamento e plugins. |
| `System.out` e `System.err` | SLF4J com Logback ou Log4j 2 | Níveis, formatação, destinos, logs estruturados e controle operacional. |
| Criação concreta de dependências em `Main` | Spring, Guice ou Dagger | Injeção de dependências e lifecycle em aplicações maiores. |
| Substituição por arquivo temporário | Transações de banco ou garantias de SDKs de storage | Durabilidade, concorrência e recuperação mais fortes. |

### Exemplo com Jackson

Com Jackson, conceitualmente o repositório teria:

```java
objectMapper.writeValue(file.toFile(), tasks);

List<Task> tasks = objectMapper.readValue(
        file.toFile(),
        new TypeReference<List<Task>>() {}
);
```

A configuração real também trataria `Instant`, construtores, campos
desconhecidos, nomes de propriedades e comportamento de erros. O projeto manual
mostra por que essas funcionalidades da biblioteca importam.

### Exemplo com JUnit

A verificação manual de exception poderia virar:

```java
assertThrows(
        IllegalArgumentException.class,
        () -> new Task(0, "Inválida")
);
```

O JUnit descobriria os testes automaticamente e Maven ou Gradle habilitaria e
executaria a suíte durante o build.

## 12. Limitações atuais e evolução para produção

O design atual é apropriado para estudo e para um único processo local. Ele não
foi projetado para múltiplas escritas concorrentes ou conjuntos grandes de dados.

Limitações importantes:

- Cada comando lê e pode reescrever o arquivo inteiro.
- Dois processos podem ler o mesmo estado e sobrescrever alterações um do outro.
- O parser aceita apenas o formato JSON conhecido pela aplicação.
- IDs podem ser reutilizados após apagar o maior ID existente.
- Não existe transação envolvendo a mudança em memória e a troca do arquivo.
- Não existe uma interface de repositório nem uma camada de serviço.
- Formatação da saída e orquestração ficam na mesma classe da CLI.
- Os testes dependem de `-ea`.

Um caminho comum de evolução seria:

1. Criar uma interface `TaskRepository`.
2. Mover casos de uso para um `TaskService`.
3. Substituir o JSON manual por Jackson ou o arquivo por um banco.
4. Deixar o armazenamento gerar os IDs.
5. Usar picocli para definir comandos.
6. Adotar JUnit 5 e um build tool.
7. Adicionar logging estruturado.
8. Adicionar transações e controle de concorrência quando necessário.

## 13. Exercícios sugeridos

1. Criar o comando `mark-todo`.
2. Criar o comando `find <id>`.
3. Ordenar a listagem por data de criação ou status.
4. Não alterar `updatedAt` quando o novo valor for igual ao antigo.
5. Criar testes permanentes para JSON inválido e IDs duplicados.
6. Extrair uma interface `TaskRepository` e testar `TaskCli` com um repositório
   fake.
7. Extrair as operações para `TaskService`.
8. Substituir o JSON manual por Jackson e comparar tamanho e comportamento.
9. Converter o projeto para Maven ou Gradle e migrar os testes para JUnit 5.
10. Substituir o JSON por SQLite ou PostgreSQL.
11. Adicionar file locking e investigar processos concorrentes.
12. Adicionar paginação para listas grandes.

## 14. Principais aprendizados

- Objetos de domínio devem proteger seu próprio estado válido.
- Parsing e validação pertencem às fronteiras.
- Persistência é necessária porque a memória do processo é temporária.
- Serialização e desserialização formam um ciclo de ida e volta.
- Exceptions sinalizam falhas; tratamento elegante também envolve tradução,
  recuperação, proteção dos dados e códigos de saída.
- Injeção de dependência pode ser apenas passar um objeto pelo construtor.
- Testar fica mais fácil quando efeitos colaterais ficam atrás de dependências.
- Diretórios temporários evitam que testes danifiquem dados reais.
- Bibliotecas e build tools automatizam fundamentos que continuam importantes de
  entender.

## 15. Arquivos relacionados

- [README do projeto](../README.md)
- [Guia de estudo em inglês](PROJECT_GUIDE.md)
- [Descrição do desafio](../challenge.md)
- [Main](../src/tasktracker/Main.java)
- [Task CLI](../src/tasktracker/cli/TaskCli.java)
- [Modelo Task](../src/tasktracker/model/Task.java)
- [Repositório JSON](../src/tasktracker/repository/JsonTaskRepository.java)
