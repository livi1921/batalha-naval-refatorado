# Minha parte - Dominio, regras de negocio e testes

Minha responsabilidade foi trabalhar no "motor" do jogo Batalha Naval, ou seja, a parte que cuida das regras sem depender da interface.

## Classes criadas

- `Coordenada`: representa uma posicao do tabuleiro, com `x`, `y`, validacao de limite e formatacao.
- `ResultadoTiro`: enum com os resultados possiveis de um tiro: `AGUA`, `ACERTO` e `AFUNDOU`.
- `Navio`: representa um navio, com nome, tamanho, vida, acerto e verificacao de afundamento.
- `Tabuleiro`: inicio da classe responsavel pelo tabuleiro e validacao de coordenadas.
- `Jogador`: representa um jogador base.
- `HumanPlayer`: representa o jogador humano.
- `CpuPlayer`: representa o jogador CPU.
- `Jogo`: inicio da classe responsavel por controlar jogadores e turno.
- `ValidadorDeFrotas`: valida se os tamanhos da frota estao corretos.

## Regras separadas no Main

Algumas regras ainda estao no `Main.java`, mas foram isoladas em metodos menores:

- `parseCoordenada`: converte texto em objeto `Coordenada`.
- `validarTamanhosDaFrota`: valida a composicao da frota.
- `podePosicionarNavio`: verifica se um navio cabe no tabuleiro.
- `temNavioEncostando`: impede navios encostados por lado ou diagonal.
- `applyShot`: aplica tiro e retorna agua, acerto ou afundou.
- `contarNaviosVivos`: conta navios ainda vivos.
- `frotaFoiAfundada`: verifica fim de jogo.

## Separacao entre interface e dominio

Os metodos de regra foram escritos sem `Scanner` e sem `System.out.println`. Assim, a leitura do teclado e as mensagens ficam na interface, enquanto as regras ficam no motor.

## Testes

Foi criado o arquivo `TESTES_MANUAIS.md` com cenarios de teste para:

- coordenadas validas e invalidas;
- posicionamento de navios;
- bloqueio de navios encostando;
- tiro na agua;
- tiro com acerto;
- navio afundado;
- fim de jogo.

## Proximo passo

O proximo passo seria mover completamente os metodos de regra que ainda estao no `Main.java` para as classes `Tabuleiro`, `Jogo` e `ValidadorDeFrotas`, alem de criar testes automatizados com JUnit.