#!/usr/bin/env python3
"""Generates data-driven content (quests, dialogues, shops, prices) in ENGLISH (source language) and the
Portuguese translation table texts_pt.json ({english: portuguese}). Run after gen_assets.py."""
import json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = os.path.join(ROOT, 'assets', 'konorpg')
D = os.path.join(ROOT, 'data', 'konorpg')
PAIRS = {}


def T(en, pt):
    PAIRS[en] = pt
    return en


def js(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)


# ------------------------------------------------------------------ quests
def quest(id, name, desc, loc='Anywhere', locpt='Qualquer lugar', humor=None, **kw):
    q = {'name': name[0], 'description': desc[0], 'location': T(loc, locpt), 'type': 'kill', 'amount': 1, 'xp': 30, 'money': 50, 'difficulty': 1,
         'minRank': 'F', 'category': 'board', 'repeatable': False}
    T(*name); T(*desc)
    if humor: q['humor'] = T(*humor)
    q.update(kw)
    js(os.path.join(D, 'konorpg_quests', id + '.json'), q)


Q = quest
Q('main_01_guild', ('Chapter 1: Welcome to the Guild', 'Capítulo 1: Bem-vindo à Guilda'),
  ('You woke up in a fantasy world. Go to the Adventurers Guild (the building north of the square), talk to the receptionist Luna and register.', 'Você acordou em um mundo de fantasia. Vá até a Guilda dos Aventureiros (prédio ao norte da praça), fale com a recepcionista Luna e registre-se.'),
  'Axel Guild', 'Guilda de Axel', ('Tip: there is a registration fee. Yes, even in the other world there is bureaucracy.', 'Dica: existe uma taxa de inscrição. Sim, até no outro mundo tem burocracia.'),
  type='event', target='register_guild', xp=50, money=100, category='main', unlocks=['main_02_class'], reputation={'guild': 2})
Q('main_02_class', ('Chapter 1: Choose Your Class', 'Capítulo 1: Escolha sua Classe'),
  ('Open the panel (K key) > GUILD and choose your class. Then learn a skill in the SKILLS tab.', 'Abra o painel (tecla K) > GUILD e escolha sua classe. Depois aprenda uma habilidade na aba SKILLS.'),
  'Axel Guild', 'Guilda de Axel', type='event', target='choose_class', xp=60, money=150, category='main', unlocks=['main_03_toads'], reputation={'guild': 2})
Q('main_03_toads', ('Chapter 1: Giant Toads', 'Capítulo 1: Sapos Gigantes'),
  ('Giant toads are swallowing adventurers on the plains. Defeat 3 of them outside town.', 'Os sapos gigantes estão engolindo aventureiros nas planícies. Derrote 3 deles fora da cidade.'),
  'Plains outside Axel', 'Planícies fora de Axel', ('Warning: toads like to swallow things. Especially goddesses.', 'Aviso: sapos gostam de engolir coisas. Principalmente deusas.'),
  target='konorpg:giant_toad', amount=3, xp=120, money=300, category='main', unlocks=['main_04_goblins'], reputation={'guild': 3, 'city': 2})
Q('main_04_goblins', ('Chapter 1: Goblin Plague', 'Capítulo 1: Praga de Goblins'), ('Goblins are stealing the crops. Defeat 6 goblins.', 'Goblins estão roubando as colheitas. Derrote 6 goblins.'),
  'Around Axel', 'Arredores de Axel', target='konorpg:goblin', amount=6, xp=160, money=400, category='main', unlocks=['main_05_gate'], difficulty=2, reputation={'guild': 3, 'city': 3})
Q('main_05_gate', ('Chapter 1: The Gates', 'Capítulo 1: Os Portões'),
  ('Visit Axel\'s south gate, the way out of town, and talk to the guards about the rumors.', 'Visite o portão sul de Axel, a saída da cidade, e converse com os guardas sobre os rumores.'),
  'Axel south gate', 'Portão sul de Axel', type='explore', target='gate', xp=80, money=200, category='main', unlocks=['main_06_skeletons'], reputation={'city': 3})
Q('main_06_skeletons', ('Chapter 1: Skeleton Warriors', 'Capítulo 1: Guerreiros Esqueleto'), ('Armed skeletons were seen at night. Defeat 5 Skeleton Warriors.', 'Esqueletos armados foram vistos à noite. Derrote 5 Guerreiros Esqueleto.'),
  'Outside town (at night)', 'Fora da cidade (à noite)', target='konorpg:skeleton_warrior', amount=5, xp=220, money=600, category='main', unlocks=['main_07_rank'], difficulty=3, reputation={'guild': 4})
Q('main_07_rank', ('Chapter 1: A Real Adventurer', 'Capítulo 1: Aventureiro de Verdade'),
  ('Do quests from the board until you reach Rank D at the Guild. (Chapter 2: dungeons - COMING SOON)', 'Faça quests no mural até alcançar o Rank D na Guilda. (Capítulo 2: dungeons - EM BREVE)'),
  'Axel Guild', 'Guilda de Axel', type='event', target='rank:D', xp=400, money=1500, category='main', difficulty=3, minLevel=5, reputation={'guild': 10, 'city': 5})

def B(id, name, desc, target, amount, xp, money, loc=('Anywhere', 'Qualquer lugar'), humor=None, type='kill', **kw):
    Q(id, name, desc, loc[0], loc[1], humor, type=type, target=target, amount=amount, xp=xp, money=money, repeatable=True, **kw)

B('b_toads', ('Giant Toad Hunt', 'Caça aos Sapos Gigantes'), ('Defeat 3 giant toads.', 'Derrote 3 sapos gigantes.'), 'konorpg:giant_toad', 3, 90, 180, ('Plains', 'Planícies'), ('Reward: money and a lesson about toads.', 'Recompensa: dinheiro e uma lição sobre sapos.'), reputation={'guild': 2})
B('b_goblins', ('Goblin Control', 'Controle de Goblins'), ('Defeat 6 goblins.', 'Derrote 6 goblins.'), 'konorpg:goblin', 6, 110, 220, ('Around town', 'Arredores'), reputation={'guild': 2})
B('b_kobolds', ('Annoying Kobolds', 'Kobolds Chatos'), ('Defeat 6 kobolds.', 'Derrote 6 kobolds.'), 'konorpg:kobold', 6, 120, 240, ('Around town', 'Arredores'), minRank='E', difficulty=2, reputation={'guild': 2})
B('b_lizardmen', ('Lizardmen', 'Homens-Lagarto'), ('Defeat 5 lizardmen.', 'Derrote 5 homens-lagarto.'), 'konorpg:lizardman', 5, 200, 380, ('Swamps and rivers', 'Pântanos e rios'), minRank='E', difficulty=2, reputation={'guild': 3})
B('b_skeletons', ('Skeleton Cleanup', 'Limpeza de Esqueletos'), ('Defeat 6 skeleton warriors.', 'Derrote 6 guerreiros esqueleto.'), 'konorpg:skeleton_warrior', 6, 220, 420, ('Fields at night', 'Campos à noite'), minRank='E', difficulty=3, reputation={'guild': 3})
B('b_orcs', ('Orc Menace', 'Ameaça Orc'), ('Defeat 4 orcs.', 'Derrote 4 orcs.'), 'konorpg:orc', 4, 300, 600, ('Forests', 'Florestas'), ('Orcs stink. The reward makes up for it. More or less.', 'Orcs fedem. A recompensa compensa. Mais ou menos.'), minRank='D', difficulty=4, reputation={'guild': 4, 'city': 2})
B('b_spiders', ('Giant Spider Nest', 'Ninho de Aranhas Gigantes'), ('Defeat 4 giant spiders.', 'Derrote 4 aranhas gigantes.'), 'konorpg:giant_spider', 4, 240, 450, ('Forests and caves', 'Florestas e cavernas'), minRank='E', difficulty=3, reputation={'guild': 3})
B('b_cabbages', ('Cabbage Harvest', 'Colheita de Repolhos'), ('Flying cabbages are worth good money. Catch 8!', 'Repolhos voadores valem bom dinheiro. Capture 8!'), 'konorpg:flying_cabbage', 8, 100, 400, ('Plains', 'Planícies'), ('They fly. They flee. They are surprisingly fast. Good luck.', 'Eles voam. Eles fogem. São surpreendentemente rápidos. Boa sorte.'), reputation={'guild': 1, 'merchants': 1})
B('b_wolves', ('Dire Wolf Pack', 'Matilha de Lobos Terríveis'), ('Defeat 4 dire wolves.', 'Derrote 4 lobos terríveis.'), 'konorpg:dire_wolf', 4, 260, 480, ('Forests', 'Florestas'), minRank='E', difficulty=3, reputation={'guild': 3})
B('b_mimics', ('Chest Inspection', 'Inspeção de Baús'), ('Some chests bite. Defeat 2 mimics.', 'Alguns baús mordem. Derrote 2 mímicos.'), 'konorpg:mimic', 2, 300, 700, ('Treasure sites', 'Locais de tesouro'), ('Never trust a chest that is just lying around.', 'Nunca confie em um baú jogado por aí.'), minRank='D', difficulty=3, reputation={'guild': 3})
B('b_golems', ('Stone Guardians', 'Guardiões de Pedra'), ('Defeat 2 golems.', 'Derrote 2 golems.'), 'konorpg:golem', 2, 400, 800, ('Mountains', 'Montanhas'), minRank='C', difficulty=4, reputation={'guild': 4})
B('b_demons', ('Demon Hunt', 'Caça aos Demônios'), ('Defeat 3 demons.', 'Derrote 3 demônios.'), 'konorpg:demon', 3, 550, 1100, ('Dark places', 'Lugares sombrios'), minRank='B', difficulty=5, reputation={'guild': 6, 'city': 2})
B('b_toad_queen', ('Slay the Toad Queen', 'Mate a Rainha dos Sapos'), ('The Giant Toad Queen rules the plains. When she appears, defeat her!', 'A Rainha dos Sapos Gigantes domina as planícies. Quando ela aparecer, derrote-a!'), 'konorpg:toad_queen', 1, 800, 2000, ('Plains (boss event)', 'Planícies (evento de chefe)'), type='boss', minRank='D', difficulty=5, cooldownMinutes=60, reputation={'guild': 10})
B('b_demon_general', ('Slay the Demon General', 'Mate o General Demoníaco'), ('The Demon General is gathering armies. When he appears, stop him!', 'O General Demoníaco está reunindo exércitos. Quando ele aparecer, detenha-o!'), 'konorpg:demon_general', 1, 1500, 4000, ('Wilderness (boss event)', 'Região selvagem (evento de chefe)'), type='boss', minRank='B', difficulty=6, cooldownMinutes=60, reputation={'guild': 15})
B('b_goblin_ears', ('Goblin Ears', 'Orelhas de Goblin'), ('The Guild pays for goblin ears. Hand in 8.', 'A Guilda paga por orelhas de goblin. Entregue 8.'), 'konorpg:goblin_ear', 8, 80, 200, humor=('Yes, it\'s gross. Yes, it pays well.', 'Sim, é nojento. Sim, paga bem.'), type='collect', reputation={'guild': 1, 'merchants': 1})
B('b_toad_legs', ('Toad Menu', 'Cardápio de Sapo'), ('The tavern wants giant toad legs. Hand in 5.', 'A taverna quer pernas de sapo gigante. Entregue 5.'), 'konorpg:toad_leg', 5, 70, 150, humor=('They say it tastes like chicken. Nobody confirmed.', 'Dizem que parece frango. Ninguém confirmou.'), type='collect', reputation={'merchants': 2})
B('b_crystals', ('Magic Crystals', 'Cristais Mágicos'), ('Hand in 2 magic crystals for the town mage.', 'Entregue 2 cristais mágicos para o Mago da cidade.'), 'konorpg:magic_crystal', 2, 250, 700, type='collect', minRank='E', difficulty=3, reputation={'guild': 2, 'merchants': 3})
B('b_bones', ('Bone Dust', 'Pó de Osso'), ('Hand in 6 bone dust for the alchemist.', 'Entregue 6 porções de pó de osso para a alquimista.'), 'konorpg:bone_dust', 6, 100, 260, type='collect', reputation={'merchants': 2})
Q('c_aqua_sake', ('Aqua and the Missing Sake', 'Aqua e o Sake Sumido'),
  ('Aqua swears she did not drink the sake. Bring 2 bottles from the shop to prove her innocence.', 'Aqua jura que não bebeu o sake. Traga 2 garrafas de sake da loja para provar a inocência dela.'),
  'Axel Tavern', 'Taverna de Axel', ('Reward: the (dubious) gratitude of the goddess.', 'Recompensa: gratidão (duvidosa) da deusa.'),
  type='collect', target='konorpg:sake_bottle', amount=2, xp=100, money=100, category='special', require={'companion': 'aqua'}, reputation={'axis': 5})

def F(id, name, desc, item, amount, money, humor=None):
    Q(id, name, desc, 'Town of Axel', 'Cidade de Axel', humor, type='collect', target=item, amount=amount, xp=25, money=money, category='favor', repeatable=True, cooldownMinutes=30, reputation={'city': 1})

F('fav_flowers', ('Flowers for the Florist', 'Flores para a Floricultura'), ('Bring 3 poppies (red flowers) to the villager.', 'Traga 3 papoulas (flores vermelhas) para o morador.'), 'minecraft:poppy', 3, 60, ('He swears it\'s "for decoration".', 'Ele jura que é para "decoração".'))
F('fav_sticks', ('Sticks for the Fireplace', 'Gravetos para a Lareira'), ('Bring 8 sticks.', 'Traga 8 gravetos.'), 'minecraft:stick', 8, 40)
F('fav_sake', ('An Axis Order Sake', 'Um Sake da Ordem Axis'), ('Bring 1 bottle of Axis Order Sake (tavern).', 'Traga 1 garrafa de Sake da Ordem Axis (taverna).'), 'konorpg:sake_bottle', 1, 150, ('They say it is "for religious purposes".', 'Dizem que é "para fins religiosos".'))
F('fav_wheat', ('Wheat for the Baker', 'Trigo para o Padeiro'), ('Bring 6 wheat.', 'Traga 6 trigos.'), 'minecraft:wheat', 6, 70)
F('fav_apples', ('Apples for the Pie', 'Maçãs para a Torta'), ('Bring 3 apples.', 'Traga 3 maçãs.'), 'minecraft:apple', 3, 80)
F('fav_cobble', ('Stones for the Wall', 'Pedras para o Muro'), ('Bring 16 cobblestone.', 'Traga 16 pedregulhos.'), 'minecraft:cobblestone', 16, 90)

# ------------------------------------------------------------------ dialogues
def node(text, choices):
    if isinstance(text, list):
        text = [T(*x) for x in text]
    else:
        text = T(*text)
    return {'text': text, 'choices': choices}


def ch(txt, next='', effects=None, **kw):
    c = {'text': T(*txt), 'next': next}
    if effects: c['effects'] = effects
    c.update(kw)
    return c


def act(a): return {'type': 'action', 'action': a}


def dlg(id, speaker, start, nodes, starts=None):
    o = {'speaker': T(*speaker), 'start': start, 'nodes': nodes}
    if starts: o['starts'] = starts
    js(os.path.join(D, 'konorpg_dialogues', id + '.json'), o)


BACK = ('Back', 'Voltar')
BYE = ('Bye.', 'Tchau.')
THANKS = ('Thank you!', 'Obrigado!')
dlg('luna', ('Luna', 'Luna'), 'back', {
    'new': node(('Welcome to the Adventurers Guild! To register, the registration fee is {fee} Eris. You have {money} Eris.', 'Bem-vindo à Guilda dos Aventureiros! Para se registrar, a taxa de inscrição é de {fee} Eris. Você tem {money} Eris.'),
                [ch(('I want to register.', 'Quero me registrar.'), 'done', [act('register')], next_fail='poor'), ch(('Just looking.', 'Só estou olhando.'))]),
    'done': node(('There you go! Here is your Adventurer Card. Open the panel (K key) > GUILD to choose your class and head to the quest board!', 'Pronto! Aqui está o seu Cartão de Aventureiro. Abra o painel (tecla K) > GUILD para escolher a classe e vá ao mural de quests!'), [ch(THANKS)]),
    'poor': node(('Hmm... the fee is {fee} Eris and you only have {money}. Adventurers without money? Common around here. Come back when you have it.', 'Hmm... a taxa é {fee} Eris e você tem só {money}. Aventureiros sem dinheiro? Comum por aqui. Volte quando tiver.'), [ch(('I see.', 'Entendi.'))]),
    'back': node([('Hello, {player}! Rank {rank}, level {level}. How can I help?', 'Olá, {player}! Rank {rank}, nível {level}. Como posso ajudar?'),
                  ('Welcome back! Any monsters in need of a lesson?', 'Bem-vindo de volta! Algum monstro precisando de uma lição?'),
                  ('Good to see you in one piece! The Guild thanks you.', 'Que bom ver você inteiro! A Guilda agradece.')], [
        ch(('Hand in completed quests (collect rewards)', 'Entregar quests concluídas (receber recompensas)'), 'paid', [act('claim_board')], next_fail='nothing'),
        ch(('Open the Guild panel (class, card)', 'Abrir o painel da Guilda (classe, cartão)'), '', [act('open_guild')]),
        ch(('How do quests work?', 'Como funcionam as quests?'), 'howto'), ch(('Adventurer tips', 'Dicas de aventureiro'), 'tips'), ch(BYE)]),
    'paid': node([('Excellent work! The Guild thanks you and the reward is already with you.', 'Excelente trabalho! A Guilda agradece e a recompensa já está com você.'), ('Mission accomplished! Your rank will thank you.', 'Missão cumprida! Seu rank vai agradecer.')], [ch(THANKS), ch(('Anything else?', 'Mais alguma coisa?'), 'back')]),
    'nothing': node(('Hmm, you have no completed quests right now. Accept quests at the board and come back when you finish!', 'Hmm, você não tem nenhuma quest concluída no momento. Aceite quests no mural e volte quando terminar!'), [ch(('I see.', 'Entendi.'), 'back')]),
    'howto': node(('Quests are on the QUEST BOARD (in the square and in here). You accept them there, complete the objective and come talk to me to get XP, Eris and reputation. The board changes over time and some quests have a cooldown.', 'Quests ficam no MURAL (praça e aqui dentro). Você aceita lá, cumpre o objetivo e vem falar comigo para receber XP, Eris e reputação. O mural muda com o tempo e algumas quests têm recarga.'), [ch(('Got it!', 'Entendi!'), 'back')]),
    'tips': node(('Tips: monsters give XP and Eris. Their items are worth money at the shops. Feed your companions from time to time and let them sleep in a bed. And never, EVER let the goddess drink before a fight.', 'Dicas: monstros dão XP e Eris. Itens deles valem dinheiro nas lojas. Alimente seus companheiros de tempos em tempos e deixe-os dormir em uma cama. E nunca, NUNCA deixe a deusa beber antes de uma luta.'), [ch(BACK, 'back')])},
    starts=[{'require': {'registered': False}, 'node': 'new'}])

dlg('shopkeeper', ('Shopkeeper', 'Lojista'), 'greet', {
    'greet': node([('Welcome! Take a look at my wares.', 'Bem-vindo! Veja minhas mercadorias.'), ('Hello, adventurer! Eris in hand, items in the inventory.', 'Olá, aventureiro! Eris na mão, itens no inventário.'), ('The stock changed today! Have a look.', 'Hoje o estoque mudou! Dê uma olhada.')],
                  [ch(('View the shop', 'Ver a loja'), '', [act('open_shop')]), ch(('Any news?', 'Alguma novidade?'), 'news'), ch(BYE)]),
    'news': node([('They say giant toads have been swallowing adventurers on the plains. Especially the ones who keep screaming.', 'Dizem que sapos gigantes andam engolindo aventureiros nas planícies. Principalmente os que ficam gritando.'),
                  ('Watch out for goblins at night. And for drunk goddesses, any time.', 'Cuidado com os goblins à noite. E com deusas bêbadas, a qualquer hora.'),
                  ('Reputation with the merchants gives discounts, you know? Selling monster items earns our trust.', 'Reputação com os comerciantes dá desconto, sabia? Vendendo itens de monstro você ganha a nossa confiança.'),
                  ('My stock changes every day. Early birds get the best items.', 'Meu estoque muda todo dia. Quem chega cedo leva os melhores itens.')], [ch(BACK, 'greet')])})
dlg('blacksmith', ('Blacksmith', 'Ferreiro'), 'greet', {
    'greet': node(('Good work needs good steel. I forge gear, repair it and reinforce it. What do you need?', 'Bom trabalho exige bom aço. Eu forjo equipamentos, conserto e reforço. O que precisa?'),
                  [ch(('Open the forge (buy, craft, repair, reinforce)', 'Abrir a forja (comprar, forjar, reparar, reforçar)'), '', [act('open_shop')]), ch(BYE)])})
dlg('alchemist', ('Alchemist', 'Alquimista'), 'greet', {
    'greet': node(('Potions! Side effects not included. I brew them from monster parts, too.', 'Poções! Efeitos colaterais não inclusos. Também preparo com partes de monstros.'),
                  [ch(('Open the lab (buy, brew potions)', 'Abrir o laboratório (comprar, preparar poções)'), '', [act('open_shop')]), ch(BYE)])})
rooms = [ch(('%d bed%s (%d Eris)' % (n, '' if n == 1 else 's', 100 + 60 * (n - 1)), '%d cama%s (%d Eris)' % (n, '' if n == 1 else 's', 100 + 60 * (n - 1))), '', [act('rent_room:%d' % n)]) for n in range(1, 7)]
rooms.append(ch(BACK, 'greet'))
dlg('innkeeper', ('Innkeeper', 'Dona da Pousada'), 'greet', {
    'greet': node([('Welcome to the inn! Resting costs 50 Eris. I also rent private rooms for sleeping, with 1 to 6 beds.', 'Bem-vindo à pousada! Descansar custa 50 Eris. Também alugo quartos privados para dormir, com 1 a 6 camas.'), ('Tired from the trip? We have soft beds.', 'Cansado da viagem? Temos camas macias.')], [
        ch(('Rest (50 Eris): heals and saves your return point', 'Descansar (50 Eris): cura e salva o ponto de retorno'), 'rested', [act('rest')], next_fail='greet'),
        ch(('Rent a private room...', 'Alugar um quarto privado...'), 'rooms'),
        ch(('Just save my return point (free)', 'Só salvar meu ponto de retorno (grátis)'), 'saved', [act('set_respawn')]), ch(BYE)]),
    'rooms': node(('How many beds? Your companions sleep in the extra ones. More beds, higher price.', 'Quantas camas? Seus companheiros dormem nas extras. Mais camas, preço maior.'), rooms),
    'rested': node(('Slept well? Your wounds and mana were restored.', 'Dormiu bem? Seus ferimentos e sua mana foram restaurados.'), [ch(THANKS)]),
    'saved': node(('Done. If you fall in battle and nobody revives you, you wake up here.', 'Pronto, se você cair em batalha e ninguém te reviver, acorda aqui.'), [ch(('Thanks.', 'Valeu.'))])})
dlg('tavern', ('Tavern Keeper', 'Taverneiro'), 'greet', {
    'greet': node([('What will it be? Fried toad legs are today\'s special! And I buy meat of all kinds.', 'O que vai ser? Pernas de sapo fritas é o prato do dia! E eu compro carnes em geral.'), ('Good evening! Here we drink, eat, gossip... and I pay well for fresh meat.', 'Boa noite! Aqui se bebe, se come, se fofoca... e eu pago bem por carne fresca.')], [
        ch(('View the menu / sell meat', 'Ver o cardápio / vender carnes'), '', [act('open_shop')]), ch(('Hear rumors', 'Ouvir rumores'), 'rumor'), ch(BYE)]),
    'rumor': node([('They say giant toads have been seen... and something bigger on the plains. A Queen, they say.', 'Dizem que sapos gigantes foram vistos... e algo maior nas planícies. Uma Rainha, dizem.'),
                   ('A blue-haired goddess was here the other day and left me the bill. But that\'s drunk talk.', 'Uma deusa de cabelo azul esteve aqui outro dia e me deixou a conta. Mas isso é papo de bêbado.'),
                   ('They say a demon general is gathering armies. I hope he\'s bad at logistics.', 'Dizem que um general demoníaco está reunindo exércitos. Espero que ele seja ruim em logística.')], [ch(BACK, 'greet')])})
dlg('guard', ('Guard', 'Guarda'), 'greet', {
    'greet': node([('Keep order in the town, adventurer.', 'Mantenha a ordem na cidade, aventureiro.'), ('Axel is safe. Almost always. Don\'t hit the townsfolk.', 'Axel é segura. Quase sempre. Não bata nos moradores.'),
                   ('The monsters outside are getting bolder. Take care.', 'Os monstros lá fora estão ficando mais ousados. Cuide-se.'), ('If you hear a goddess screaming, the problem is probably a toad.', 'Se ouvir gritos de deusa, o problema provavelmente é um sapo.')], [
        ch(('Rumors?', 'Rumores?'), 'rumor'), ch(('Where is the Guild?', 'Onde fica a Guilda?'), 'guild'), ch(BYE)]),
    'rumor': node([('Lately the undead have been showing up at night. A priest would help... or a goddess. If she\'s sober.', 'Ultimamente, mortos-vivos têm surgido à noite. Um padre faria bem... ou uma deusa. Se ela estiver sóbria.'),
                   ('People talk about monsters ambushing travelers outside the walls. Stay alert.', 'Falam de monstros emboscando viajantes fora dos muros. Fique atento.')], [ch(('I see.', 'Entendi.'))]),
    'guild': node(('The Guild is the big building north of the square. The quest board is in the square and inside it.', 'A Guilda é o prédio grande ao norte da praça. O mural de quests fica na praça e dentro dela.'), [ch(THANKS)])})

def favor(id, speaker, quest_id, greet, ask, waiting, done):
    chat = [('Axel is peaceful... except when a certain goddess shows up.', 'Axel é pacata... exceto quando uma certa deusa aparece.'),
            ('The monsters outside keep showing up. Good thing we have the Guild.', 'Os monstros lá fora não param de aparecer. Ainda bem que temos a Guilda.'),
            ('They say the Guild has a new quest on the board all the time.', 'Dizem que a Guilda tem um mural novo toda hora.'),
            ('I heard about other towns beyond the hills. One day I\'ll go there!', 'Ouvi falar de outras cidades além das colinas. Um dia eu vou lá!')]
    dlg(id, speaker, 'greet', {
        'greet': node(greet, [ch(('Can I help with something?', 'Posso ajudar com alguma coisa?'), 'offer'), ch(('Chat', 'Conversar'), 'chat'), ch(BYE)]),
        'offer': node(ask, [ch(('Sure, I\'ll fetch it!', 'Claro, eu busco!'), '', [{'type': 'start_quest', 'id': quest_id}, {'type': 'msg', 'text': T('Favor accepted! (Hand it in by talking to the person again.)', 'Favor aceito! (Entregue falando com a pessoa de novo.)')}]), ch(('Not now.', 'Agora não.'))]),
        'waiting': node(waiting, [ch(('I\'ll be right back.', 'Já volto.'))]),
        'deliver': node(('You brought it! Thank you very much, adventurer.', 'Você trouxe! Muito obrigado, aventureiro.'), [ch(('Hand it over', 'Entregar'), 'thanks', [{'type': 'claim_quest', 'id': quest_id}])]),
        'thanks': node(done, [ch(('You\'re welcome!', 'De nada!'))]),
        'chat': node(chat, [ch(BACK, 'greet')])},
        starts=[{'require': {'quest_ready': quest_id}, 'node': 'deliver'}, {'require': {'quest_active': quest_id}, 'node': 'waiting'}])


favor('villager_flowers', ('Villager', 'Moradora'), 'fav_flowers', [('Good morning! The sun is lovely today.', 'Bom dia! O sol está lindo hoje.'), ('Hello, traveler! Do you like flowers?', 'Olá, viajante! Gosta de flores?')],
      ('I want to decorate my house... could you bring me 3 poppies?', 'Quero decorar a casa... Você pode me trazer 3 papoulas?'), ('I still need those 3 poppies!', 'Ainda preciso daquelas 3 papoulas!'), ('They look lovely! Here is your reward.', 'Ficaram lindas! Aqui está sua recompensa.'))
favor('villager_sticks', ('Villager', 'Morador'), 'fav_sticks', [('Brr, it\'s cold!', 'Eita, que frio!'), ('Hey, adventurer!', 'Opa, aventureiro!')],
      ('The fireplace is dying! Could you bring me 8 sticks?', 'A lareira está apagando! Pode me trazer 8 gravetos?'), ('The 8 sticks... I need them before the cold.', 'Os 8 gravetos... preciso deles antes do frio.'), ('Now the house will be nice and warm. Thanks!', 'Agora a casa vai ficar quentinha. Valeu!'))
favor('villager_sake', ('Villager', 'Morador'), 'fav_sake', [('Psst, you there.', 'Psiu, você aí.'), ('Hey, stranger...', 'Ei, forasteiro...')],
      ('Psst... could you get me an Axis Order Sake? It\'s at the tavern. It\'s for a... ceremony.', 'Psiu... conseguiria um Sake da Ordem Axis para mim? Está na taverna. É para uma... cerimônia.'), ('The sake! I\'m getting thirsty... I mean, faithful.', 'O sake! Já estou com sede... quer dizer, com fé.'), ('Glug glug... I mean, blessed be!', 'Glub glub... digo, abençoado seja!'))
favor('villager_wheat', ('Baker', 'Padeiro'), 'fav_wheat', [('Smells like bread, right?', 'Cheirinho de pão, né?'), ('Good morning, good morning!', 'Bom dia, bom dia!')],
      ('I ran out of wheat. Bring 6 wheat and I\'ll pay you well!', 'Meu estoque de trigo acabou. Traga 6 trigos e eu lhe pago bem!'), ('Still waiting for the wheat. No wheat, no bread.', 'Ainda aguardo o trigo. Sem trigo, sem pão.'), ('Fresh bread tomorrow, thanks to you!', 'Pão fresquinho amanhã, graças a você!'))
favor('villager_apples', ('Villager', 'Moradora'), 'fav_apples', [('Hi! You look hungry.', 'Oi! Você parece faminto.'), ('Hello! Have you tried the tavern\'s pie?', 'Olá! Já provou a torta da taverna?')],
      ('I\'m making a pie, but I\'m missing 3 apples. Can you fetch some?', 'Vou fazer uma torta, mas faltam 3 maçãs. Pode buscar?'), ('The apples... the apple trees are outside the walls!', 'As maçãs... as macieiras ficam fora dos muros!'), ('What a pie this will be! Want a slice? No? Then take this.', 'Que torta vai ficar! Quer um pedaço? Não? Então toma isto.'))
favor('villager_gossip', ('Villager', 'Morador'), 'fav_cobble', [('Did you hear? They say a goblin stole a chicken.', 'Já ouviu? Dizem que um goblin roubou uma galinha.'), ('Careful on the plains: giant toads swallow people.', 'Cuidado com as planícies: sapos gigantes engolem gente.'), ('I saw a goddess crying near the fountain...', 'Eu vi uma deusa chorando perto da fonte...')],
      ('My yard wall collapsed. Can you get 16 cobblestone?', 'O muro do meu quintal desabou. Consegue 16 pedregulhos?'), ('I still need the 16 cobblestone!', 'Ainda preciso dos 16 pedregulhos!'), ('My wall will be sturdy again!', 'Meu muro vai ficar firme de novo!'))
dlg('villager', ('Villager', 'Morador'), 'greet', {'greet': node([('Good day! Are you new here? Beware of toads.', 'Bom dia! Você é novo por aqui? Cuidado com os sapos.'), ('I wanted to be an adventurer, but I\'m afraid of toads. Very afraid.', 'Eu queria ser aventureiro, mas tenho medo de sapos. Muito medo.')], [ch(BYE)])})
dlg('wounded', ('Wounded Adventurer', 'Aventureiro Ferido'), 'greet', {
    'greet': node(('Ugh... a monster got me... could you help me? A healer\'s fee is 60 Eris...', 'Ugh... um monstro me pegou... pode me ajudar? A taxa de um curandeiro é 60 Eris...'), [
        ch(('Pay 60 Eris to heal him', 'Pagar 60 Eris para curá-lo'), 'thanks', [{'type': 'money', 'amount': -60}, {'type': 'rep', 'faction': 'city', 'amount': 3}, {'type': 'xp', 'amount': 40}, {'type': 'give', 'item': 'konorpg:health_potion', 'count': 1}], next_fail='greet'),
        ch(('Leave him', 'Deixá-lo'))]),
    'thanks': node(('Thank you, kind soul! Take this potion as thanks. I owe you one!', 'Obrigado, alma bondosa! Fique com esta poção como agradecimento. Eu lhe devo uma!'), [ch(('Take care.', 'Cuide-se.'))])})
dlg('aqua', ('Aqua', 'Aqua'), 'talk', {
    'talk': node([('Yes? Speak to the goddess. Quickly, I\'m busy. (I\'m not.)', 'Sim? Fale com a deusa. Rápido, estou ocupada. (Não estou.)'), ('Hi! Came to buy me a drink? No? Dang.', 'Oi! Veio me pagar uma bebida? Não? Que droga.'), ('Speak, humble adventurer.', 'Pode falar, humilde aventureiro.')], [
        ch(('Aqua, heal me!', 'Aqua, me cure!'), 'healed', [act('companion_heal'), {'type': 'friendship', 'id': 'aqua', 'amount': 1}], next_fail='talk'),
        ch(('Here, food! (feed her the item in your hand)', 'Toma, comida! (alimentar com o item da mão)'), 'fed', [act('companion_feed')], next_fail='talk'),
        ch(('How are you?', 'Como você está?'), 'how'), ch(('How did you sleep?', 'Como você dormiu?'), 'sleep'),
        ch(('Tell me about the Axis Order.', 'Me fale da Ordem Axis.'), 'axis', require={'friendship': {'id': 'aqua', 'min': 2}}),
        ch(('Tell me a goddess story.', 'Conte uma história de deusa.'), 'story', require={'friendship': {'id': 'aqua', 'min': 3}}),
        ch(('Stay here / Follow me', 'Fique aqui / Siga-me'), '', [act('companion_stay')]), ch(('I need you to go away.', 'Preciso que você vá embora.'), 'refuse'), ch(BYE)]),
    'healed': node(('Heal! You can thank me later. (I can only do this once in a while, okay? I\'m a goddess, not a fountain.)', 'Heal! Pode agradecer depois. (Eu só consigo fazer isso de tempos em tempos, tá? Sou uma deusa, não uma fonte.)'), [ch(('Thanks, Aqua.', 'Valeu, Aqua.'), 'talk')]),
    'fed': node([('Mmm! That was great! You really know how to take care of a goddess.', 'Hmm! Isso estava ótimo! Você realmente sabe cuidar de uma deusa.'), ('Nom nom... if you have sake, I\'ll take that too!', 'Nham nham... se tiver sake, eu aceito também!')], [ch(('You\'re welcome.', 'De nada.'), 'talk')]),
    'how': node([('Me? Wonderful, of course! I\'m just thirsty... and hungry... and I miss heaven.', 'Eu? Maravilhosa, claro! Só estou com sede... e com fome... e com saudade do céu.'), ('I\'m great! But if a giant toad shows up, I\'m hiding behind you. Deal?', 'Estou ótima! Mas se aparecer um sapo gigante, eu me escondo atrás de você. Combinado?')], [ch(BACK, 'talk')]),
    'sleep': node(('Terribly! A goddess needs a real bed, you know. Beds, not floors! Rent a room at the inn when we are tired.', 'Péssimo! Uma deusa precisa de uma cama de verdade, sabia? Camas, não chão! Alugue um quarto na pousada quando estivermos cansados.'), [ch(BACK, 'talk')]),
    'axis': node(('The Axis Order adores me! It\'s the biggest religion in the world... or is it the worst reputation in the world? Nobody knows. But they give me gifts, sometimes.', 'A Ordem Axis me adora! É a maior religião do mundo... ou será que é a pior reputação do mundo? Ninguém sabe. Mas eles me dão presentes, às vezes.'), [ch(BACK, 'talk', [{'type': 'rep', 'faction': 'axis', 'amount': 1}])]),
    'story': node(('Once I purified an entire lake. It got so clean the fish left. They say that\'s my power... I say it was a victory.', 'Uma vez eu purifiquei um lago inteiro. Ficou tão limpo que os peixes foram embora. Dizem que é o meu poder... eu digo que foi uma vitória.'), [ch(BACK, 'talk', [{'type': 'friendship', 'id': 'aqua', 'amount': 1}])]),
    'refuse': node(('What?! I\'m a GODDESS! I\'m not going anywhere! You brought me to this world, now deal with it!', 'Como é?! Eu sou uma DEUSA! Não vou a lugar nenhum! Você me trouxe para este mundo, agora aguenta!'), [ch(('Sorry...', 'Desculpe...'), 'talk', [{'type': 'friendship', 'id': 'aqua', 'amount': -1}])])})

# ------------------------------------------------------------------ shops
def shop(id, name, buy, recipes=None, sellfilter=None, repair=False, reinforce=False):
    o = {'name': T(*name), 'sell': True, 'buy': [({'item': i, 'price': p, 'rotate': True} if r else {'item': i, 'price': p}) for (i, p, r) in buy]}
    if recipes: o['recipes'] = [{'result': r, 'count': c, 'cost': cost, 'ingredients': [{'item': a, 'count': b} for a, b in ings]} for (r, c, cost, ings) in recipes]
    if sellfilter: o['sellFilter'] = sellfilter
    if repair: o['repair'] = True
    if reinforce: o['reinforce'] = True
    js(os.path.join(D, 'konorpg_shops', id + '.json'), o)


K = 'konorpg:'
shop('weapons', ("Bork's Weapons", 'Armas do Bork'), [(K + 'adventurer_sword', 250, False), ('minecraft:arrow', 4, False), (K + 'goblin_hatchet', 400, True), (K + 'knight_longsword', 1200, True),
      (K + 'crusader_blade', 4000, True), (K + 'hunter_bow', 900, True), ('minecraft:iron_sword', 200, True), ('minecraft:bow', 150, True)])
shop('armor', ("Greta's Armor", 'Armaduras da Greta'), [(K + 'adventurer_helmet', 80, False), (K + 'adventurer_chestplate', 140, False), (K + 'adventurer_leggings', 110, False), (K + 'adventurer_boots', 70, False), ('minecraft:shield', 220, False),
      (K + 'knight_helmet', 400, True), (K + 'knight_chestplate', 700, True), (K + 'knight_leggings', 550, True), (K + 'knight_boots', 350, True),
      (K + 'crusader_helmet', 1400, True), (K + 'crusader_chestplate', 2400, True), (K + 'crusader_leggings', 1900, True), (K + 'crusader_boots', 1200, True)])
shop('magic', ("Wiz's Magic", 'Magia do Wiz'), [(K + 'mana_potion', 90, False), (K + 'apprentice_staff', 500, False), (K + 'mage_helmet', 300, False), (K + 'mage_chestplate', 520, False), (K + 'mage_leggings', 420, False), (K + 'mage_boots', 260, False),
      (K + 'greater_mana_potion', 280, True), (K + 'archmage_staff', 6000, True), (K + 'mana_ring', 1500, True), (K + 'flame_necklace', 2500, True), (K + 'lucky_ring', 2200, True), (K + 'magic_crystal', 260, True)],
     recipes=[(K + 'apprentice_staff', 1, 100, [('minecraft:stick', 3), (K + 'magic_crystal', 1)]), (K + 'archmage_staff', 1, 1500, [(K + 'magic_crystal', 4), (K + 'demon_horn', 2), ('minecraft:stick', 2)]),
              (K + 'mana_ring', 1, 300, [('minecraft:iron_ingot', 2), (K + 'magic_crystal', 2)]), (K + 'mage_helmet', 1, 100, [('minecraft:string', 6), (K + 'magic_crystal', 1)]),
              (K + 'mage_chestplate', 1, 200, [('minecraft:string', 10), (K + 'magic_crystal', 2)]), (K + 'mage_leggings', 1, 150, [('minecraft:string', 8), (K + 'magic_crystal', 1)]),
              (K + 'mage_boots', 1, 100, [('minecraft:string', 5), (K + 'magic_crystal', 1)])])
shop('items', ("Tomás's Items", 'Itens do Tomás'), [(K + 'health_potion', 80, False), (K + 'return_scroll', 150, False), ('minecraft:bread', 10, False), ('minecraft:torch', 3, False),
      (K + 'greater_health_potion', 250, True), (K + 'iron_ring', 700, True), (K + 'swift_ring', 1500, True), (K + 'guardian_ring', 1500, True), ('minecraft:apple', 8, True), ('minecraft:cooked_chicken', 18, True)])
shop('smith', ("Grom's Forge", 'Forja do Grom'), [('minecraft:iron_ingot', 40, False), ('minecraft:coal', 6, False), ('minecraft:iron_sword', 220, False), ('minecraft:iron_pickaxe', 220, False), ('minecraft:iron_axe', 220, False), ('minecraft:leather', 12, False)],
     recipes=[(K + 'goblin_hatchet', 1, 100, [('minecraft:iron_ingot', 3), (K + 'goblin_ear', 4)]), (K + 'knight_longsword', 1, 300, [('minecraft:iron_ingot', 6), (K + 'orc_tusk', 2)]),
              (K + 'crusader_blade', 1, 1500, [('minecraft:iron_ingot', 10), (K + 'magic_crystal', 2), (K + 'orc_tusk', 3)]), (K + 'hunter_bow', 1, 100, [('minecraft:string', 4), ('minecraft:stick', 6), (K + 'kobold_fang', 2)]),
              (K + 'adventurer_helmet', 1, 40, [('minecraft:leather', 5)]), (K + 'adventurer_chestplate', 1, 70, [('minecraft:leather', 8)]), (K + 'adventurer_leggings', 1, 55, [('minecraft:leather', 7)]), (K + 'adventurer_boots', 1, 35, [('minecraft:leather', 4)]),
              (K + 'knight_helmet', 1, 200, [('minecraft:iron_ingot', 5)]), (K + 'knight_chestplate', 1, 350, [('minecraft:iron_ingot', 8), (K + 'orc_tusk', 1)]), (K + 'knight_leggings', 1, 280, [('minecraft:iron_ingot', 7)]), (K + 'knight_boots', 1, 180, [('minecraft:iron_ingot', 4)]),
              (K + 'crusader_helmet', 1, 800, [('minecraft:iron_ingot', 6), (K + 'golem_core', 1)]), (K + 'crusader_chestplate', 1, 1500, [('minecraft:iron_ingot', 10), (K + 'golem_core', 2)]),
              (K + 'crusader_leggings', 1, 1100, [('minecraft:iron_ingot', 8), (K + 'golem_core', 1)]), (K + 'crusader_boots', 1, 700, [('minecraft:iron_ingot', 5), (K + 'golem_core', 1)])],
     repair=True, reinforce=True)
shop('alchemy', ("Lyra's Alchemy", 'Alquimia da Lyra'), [(K + 'health_potion', 75, False), (K + 'mana_potion', 85, False), (K + 'greater_health_potion', 235, False), (K + 'greater_mana_potion', 265, False), (K + 'magic_crystal', 200, True), (K + 'spider_gland', 60, True)],
     recipes=[(K + 'health_potion', 2, 20, [(K + 'toad_leg', 2), (K + 'bone_dust', 1)]), (K + 'mana_potion', 2, 30, [(K + 'magic_crystal', 1), (K + 'goblin_ear', 2)]),
              (K + 'greater_health_potion', 1, 60, [(K + 'health_potion', 2), (K + 'spider_gland', 1)]), (K + 'greater_mana_potion', 1, 90, [(K + 'mana_potion', 2), (K + 'magic_crystal', 1)]),
              (K + 'return_scroll', 1, 50, [('minecraft:paper', 1), (K + 'magic_crystal', 1)])])
MEATS = ['minecraft:beef', 'minecraft:cooked_beef', 'minecraft:porkchop', 'minecraft:cooked_porkchop', 'minecraft:mutton', 'minecraft:cooked_mutton', 'minecraft:chicken', 'minecraft:cooked_chicken',
         'minecraft:rabbit', 'minecraft:cooked_rabbit', 'minecraft:cod', 'minecraft:cooked_cod', 'minecraft:salmon', 'minecraft:cooked_salmon', K + 'toad_leg', K + 'fried_toad_leg']
shop('tavern', ("Joe's Tavern", 'Taverna do Joe'), [(K + 'fried_toad_leg', 30, False), (K + 'sake_bottle', 120, False), ('minecraft:bread', 8, False), ('minecraft:cooked_beef', 20, True), ('minecraft:cooked_porkchop', 18, True), ('minecraft:apple', 7, True)], sellfilter=MEATS)
shop('wandering', ('Wandering Merchant', 'Mercador Ambulante'), [(K + 'health_potion', 90, True), (K + 'mana_potion', 100, True), (K + 'return_scroll', 170, True), (K + 'greater_health_potion', 290, True), (K + 'lucky_ring', 2500, True),
      (K + 'swift_ring', 1700, True), (K + 'mage_chestplate', 600, True), (K + 'knight_helmet', 450, True), (K + 'golem_core', 300, True), (K + 'sake_bottle', 140, True)])
prices = {'konorpg:goblin_ear': 12, 'konorpg:kobold_fang': 10, 'konorpg:orc_tusk': 35, 'konorpg:lizard_scale': 25, 'konorpg:bone_dust': 8, 'konorpg:magic_crystal': 80, 'konorpg:toad_leg': 8,
          'konorpg:fried_toad_leg': 18, 'konorpg:cabbage': 40, 'konorpg:sake_bottle': 40, 'konorpg:golem_core': 150, 'konorpg:demon_horn': 90, 'konorpg:wolf_fang': 30, 'konorpg:mimic_tooth': 70,
          'konorpg:spider_gland': 25, 'konorpg:demon_sigil': 600, 'minecraft:iron_ingot': 15, 'minecraft:gold_ingot': 30, 'minecraft:diamond': 120, 'minecraft:emerald': 50, 'minecraft:bone': 4, 'minecraft:string': 3,
          'minecraft:gunpowder': 8, 'minecraft:rotten_flesh': 1, 'minecraft:iron_nugget': 2, 'minecraft:coal': 3, 'minecraft:poppy': 4, 'minecraft:stick': 1, 'minecraft:wheat': 3, 'minecraft:apple': 5,
          'minecraft:cobblestone': 1, 'minecraft:leather': 6, 'minecraft:gold_nugget': 4}
for m, v in {'beef': 6, 'cooked_beef': 14, 'porkchop': 6, 'cooked_porkchop': 14, 'mutton': 5, 'cooked_mutton': 12, 'chicken': 4, 'cooked_chicken': 10, 'rabbit': 5, 'cooked_rabbit': 11, 'cod': 4, 'cooked_cod': 9, 'salmon': 5, 'cooked_salmon': 10}.items():
    prices['minecraft:' + m] = v
js(os.path.join(D, 'konorpg_economy', 'sell_prices.json'), prices)

# ------------------------------------------------------------------ Java-side strings
import i18n.java_pairs as jp
for en, pt in jp.PAIRS.items():
    PAIRS[en] = pt
js(os.path.join(A, 'lang', 'texts_pt.json'), PAIRS)
print('data generated:', len(PAIRS), 'texts')
