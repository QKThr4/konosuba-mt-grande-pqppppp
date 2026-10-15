# KonoRPG - Blessings on this Wonderful Guild (versão de TESTE 0.4.0)

Mod de RPG para **Minecraft 1.21.1 + NeoForge 21.1.x** inspirado em KonoSuba.

## Como gerar o .jar (GitHub)
1. Crie um repositório e envie TODO o conteúdo desta pasta (inclusive a pasta `.github`).
2. Abra a aba **Actions** > "Build mod" > o build roda sozinho (ou "Run workflow").
3. Baixe o artefato **konorpg-jar** (o .jar fica dentro). Coloque em `mods/`.
4. Se o build falhar, copie o erro e mande para o Claude.

(Local: instale Gradle 8.10+ e JDK 21, rode `gradle build`; o jar sai em `build/libs`.)

## Como jogar (teste)
- Mundo novo: você acorda na **sala branca** da deusa Aqua. Ela oferece UM presente permanente. Depois de escolher, ela lança o feitiço de teleporte (se escolher a própria Aqua, a deusa Eris assume o lugar dela e Aqua vai junto, como sua companheira/pet).
- Você chega em **Axel** (cidade grande com muralha, casas com moradores, lojas). Vá à **Guilda** (norte da praça), fale com **Luna**, pague a taxa (1000 Eris) e receba o Cartão.
- **Quests**: só se aceita no **mural** (praça e dentro da Guilda). As recompensas só são pagas falando com a **Luna**. O mural muda com o tempo e quests têm recarga. Moradores dão pequenos favores (flores, gravetos, sake...) e pagam eles mesmos.
- **K** painel (botão "Verificar" nas quests), **J** HUD de progresso, **Z X C V** habilidades.
- **Aqua/companheiros**: seguem você (teleporte ON/OFF no painel), só atacam o que você bate / o que bate em você, sacam a arma só em combate, precisam ser **alimentados** (sem comer ~20 min: -50%; ~40 min: -80% e lenta). Se você cair, eles vêm te reviver em até 1 min (vida cheia); eles também se revivem.
- Olhe para qualquer mob/NPC para ver a vida dele no topo da tela.
- Comandos: `/konorpg city`, `/konorpg hub`, `/konorpg xp <n>`, `/konorpg money <n>`, `/konorpg quest <id>`, `/konorpg reset`, `/konorpg intro` (op).

## Novidades 0.4.0
- Mundo NOVO é necessário para ver a cidade/sala novas (a cidade antiga não muda).
- Hotel: fale com a dona da pousada e alugue um quarto (150 Eris). Moradores dormem em camas à noite; Aqua dorme na cama ao lado (ou reclama do chão).
- Melhorar habilidades (aba SKILLS) com SP: +25% poder e menos recarga por nível (máx. 5).
- A sala do céu some depois da intro (volta se outro jogador chegar; se Aqua já foi escolhida, quem recebe é a Eris).

## Novidades 0.4.0
- Tradução EN/PT automática conforme o idioma do jogo (textos em tools/i18n e tools/gen_data.py).
- Companheiros: cansaço sem dormir em cama; hotel com 1 a 6 camas.
- Eventos aleatórios (emboscada, mercador, aventureiro ferido, tesouro/mímico, chefe, incidente da Aqua, invasão).
- Crafting e serviços (reparar/reforçar) no ferreiro/alquimista/mago; armaduras RPG (4 conjuntos).
- Monstros novos: Aranha Gigante, Lobo Terrível, Repolho Voador, Mímico, Golem, Demônio; chefes: Rainha dos Sapos e General Demoníaco.

## EM BREVE
Dungeons (estruturas), outras cidades, dragão e Lich King, (ainda não implementado)
Dungeons e chefes, mimic/cabbage/aranha/golem/demônio/dragão, outras cidades pelo mundo, eventos aleatórios, missões de grupo da guilda, criar a própria guilda,
recrutar aventureiros, crafting/serviços do ferreiro e alquimista, armaduras RPG, rotinas de sono/taverna dos NPCs, classes avançadas
(skills do Crusader, Sword Master, High Priest, Assassin, Necromancer, Battle Mage, Paladin) e modelo próprio do sapo gigante.

## Estrutura
`src/main/java/com/konorpg/...` (registry, data, rpg, skill, quest, dialogue, shop, entity, world, network, event, client).
Quests, diálogos, lojas e preços ficam em `src/main/resources/data/konorpg/` (JSON). `tools/gen_assets.py` regenera texturas/JSON.
