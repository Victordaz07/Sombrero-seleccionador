# Guion de grabación del sombrero

Cada audio va en `app/src/main/assets/voz/<archivo>` en formato **MP3**.

Este archivo se genera solo con `python3 herramientas/generar_voces.py --solo-lista`.

## Discursos completos (recomendado)

4 por casa. Cada uno se graba **de corrido**, en un solo audio. La app los va rotando para que no se repitan.
Haz una pausa de suspenso antes del nombre de la casa (en **negritas**) y grítalo con emoción.

| Archivo | Discurso |
|---|---|
| `gryffindor_discurso_1.mp3` | [excited] ¡Ajá! ¡Pero qué tenemos aquí! [mischievously] Valor, coraje... ¡y un corazón que NO se rinde! [laughs] ¡Esta cabeza está que arde! [excited] ¡No hay duda! Tu lugar está en... **[shouting] ¡GRÍFINDOR!** |
| `gryffindor_discurso_2.mp3` | [curious] Hmm, hmm, hmm... ¡a ver! [excited] ¡Aventura! ¡Retos! ¡Ganas de comerte el mundo! [impressed] Y siempre defiendes a los demás... [excited] ¡Ya sé! ¡Ya sé a dónde vas! **[shouting] ¡GRÍFINDOR!** |
| `gryffindor_discurso_3.mp3` | [laughs] ¡Ja! ¿Nervios? ¡Pero si aquí adentro hay un león! [excited] ¡Das el paso aunque tengas miedo! [mischievously] Eso... eso es justo lo que yo busco. [excited] Te envío a... **[shouting] ¡GRÍFINDOR!** |
| `gryffindor_discurso_4.mp3` | [excited] ¡Oh, oh, oh! ¡Esta me la sé! [mischievously] Corazón noble, palabra firme... ¡y mucho, MUCHO valor! [excited] ¡Ni lo pienso dos veces! **[shouting] ¡GRÍFINDOR!** |
| `hufflepuff_discurso_1.mp3` | [curious] Hmm... ¡a ver, a ver! [surprised] ¡Oh! ¡Qué corazón tan grande! [excited] Leal, amable... ¡y trabajas como nadie! [excited] ¡Ya lo tengo! Tu lugar está en... **[shouting] ¡JÁFELPAF!** |
| `hufflepuff_discurso_2.mp3` | [laughs] ¡Ja, ja! ¡Compartes tus dulces de Halloween! [impressed] ¿Hasta el último chocolate? ¡Eso es bondad de verdad! [excited] ¡Está clarísimo! Perteneces a... **[shouting] ¡JÁFELPAF!** |
| `hufflepuff_discurso_3.mp3` | [curious] Mmm... ¿astucia? ¿valor? [mischievously] ¡No, no, no! Hay que mirar más profundo... [excited] ¡Un corazón leal que NUNCA abandona a nadie! [excited] ¡Ya está! **[shouting] ¡JÁFELPAF!** |
| `hufflepuff_discurso_4.mp3` | [excited] ¡Ah, qué maravilla! [laughs] ¡Esta cabeza huele a pan recién horneado! [impressed] Haces que todos se sientan en casa... ¡y nunca te rindes! [excited] Tu casa es... **[shouting] ¡JÁFELPAF!** |
| `ravenclaw_discurso_1.mp3` | [excited] ¡Vaya! ¡Cuántas preguntas! [curious] ¿Por qué el cielo es azul? ¿Cómo vuelan los murciélagos? [impressed] ¡Una mente que nunca se apaga! [excited] ¡Lo sé! Tu lugar está en... **[shouting] ¡RÉIVENCLO!** |
| `ravenclaw_discurso_2.mp3` | [curious] Hmm, hmm... ¡interesante! [impressed] Escuchas, observas... ¡y encuentras soluciones donde nadie las ve! [mischievously] Podría mandarte con los valientes... pero no. [excited] Te envío a... **[shouting] ¡RÉIVENCLO!** |
| `ravenclaw_discurso_3.mp3` | [surprised] ¡Uy! ¡Cuidado, que me haces pensar de más! [excited] Colores, historias, inventos... ¡mundos enteros aquí adentro! [laughs] ¡Creatividad pura! [excited] Perteneces a... **[shouting] ¡RÉIVENCLO!** |
| `ravenclaw_discurso_4.mp3` | [mischievously] Ajá... mientras todos corren, tú piensas. [impressed] ¡Y casi siempre tienes razón! [excited] ¡Sabiduría! ¡Ingenio! ¡No hay ninguna duda! **[shouting] ¡RÉIVENCLO!** |
| `slytherin_discurso_1.mp3` | [mischievously] ¡Ajá! ¡Aquí hay grandes planes! [excited] ¡Enormes! Cuando te propones algo... ¡NADA te detiene! [impressed] Y los demás te siguen... [excited] Tu lugar está en... **[shouting] ¡SLÍDERIN!** |
| `slytherin_discurso_2.mp3` | [laughs] ¡Ja! ¡Ya te vi pensando cómo convencerme! [mischievously] ¡Qué mente tan astuta! [impressed] Planeas cada paso, como en el ajedrez... [excited] ¡Muy bien, me convenciste! **[shouting] ¡SLÍDERIN!** |
| `slytherin_discurso_3.mp3` | [curious] Hmm... ¿un gran corazón? Sí, sí... [excited] ¡Pero qué voluntad de hierro! [impressed] Te caes... ¡y te levantas con más fuerza! [excited] ¡Decidido! Perteneces a... **[shouting] ¡SLÍDERIN!** |
| `slytherin_discurso_4.mp3` | [laughs] ¡Ja, ja! ¡Ideas brillantes... y un poquito traviesas! [mischievously] Organizas el juego, pones las reglas... ¡y cuidas a los tuyos! [excited] ¡Un líder nato! Tu casa es... **[shouting] ¡SLÍDERIN!** |

## Frases sueltas (respaldo)

Si una casa no tiene discursos grabados, el sombrero arma el discurso con estas frases.
Si una frase no tiene audio, la app usa la voz del teléfono para esa frase.

| Archivo | Frase |
|---|---|
| `prueba.mp3` | Hmm... ¿me escuchas bien? Soy el sombrero seleccionador. |
| `apertura_1.mp3` | Mmm... veamos, veamos... ¿qué tenemos aquí? |
| `apertura_2.mp3` | Ah... una cabeza nueva. No te muevas, que voy a mirar muy adentro. |
| `apertura_3.mp3` | Interesante... muy interesante. Déjame pensar... |
| `apertura_4.mp3` | Hmm... llevo cientos de años haciendo esto, y aun así me sorprendes. |
| `apertura_5.mp3` | Bien, bien... no tengas miedo. Solo voy a echar un vistazo a tus pensamientos. |
| `apertura_6.mp3` | Oh, vaya... ¡cuántas cosas hay en esta cabeza! |
| `decision_1.mp3` | Sí... ahora lo veo con claridad. |
| `decision_2.mp3` | Ya no tengo ninguna duda. |
| `decision_3.mp3` | Lo he decidido. |
| `decision_4.mp3` | Mmm... sí, sí. Está muy claro. |
| `decision_5.mp3` | Muy bien... sé exactamente a dónde perteneces. |
| `preanuncio_1.mp3` | Tu lugar está en... |
| `preanuncio_2.mp3` | Tu casa será... |
| `preanuncio_3.mp3` | Te envío a... |
| `preanuncio_4.mp3` | Perteneces a... |
| `cierre_1.mp3` | ¡Que tengas una noche mágica! |
| `cierre_2.mp3` | ¡Tu nueva casa te recibe con los brazos abiertos! |
| `cierre_3.mp3` | ¡Ve con tu casa, que la fiesta te espera! |
| `cierre_4.mp3` | ¡Que la alegría te acompañe esta noche! |
| `cierre_5.mp3` | ¡Siguiente, por favor! |
| `gryffindor_virtud_1.mp3` | Veo un valor que no se rinde, ni siquiera cuando tienes miedo. |
| `gryffindor_virtud_2.mp3` | Tienes un corazón noble: defiendes a los demás aunque nadie te lo pida. |
| `gryffindor_virtud_3.mp3` | Te gustan los retos... y cuando todos dan un paso atrás, tú das uno al frente. |
| `gryffindor_virtud_4.mp3` | Hay fuego en tu interior. Un fuego que contagia valor a quienes te rodean. |
| `gryffindor_virtud_5.mp3` | Dices la verdad aunque cueste. Eso es verdadera valentía. |
| `gryffindor_virtud_6.mp3` | Veo una gran capacidad para levantarte cada vez que caes. |
| `gryffindor_duda_1.mp3` | Aunque también veo mucha valentía... esto no será tan fácil. |
| `gryffindor_duda_2.mp3` | Pero espera... también hay un coraje enorme aquí. Hmm... |
| `gryffindor_duda_3.mp3` | Y por aquí encuentro un espíritu aventurero... ¡qué difícil decisión! |
| `gryffindor_anuncio.mp3` | ¡Grífindor! |
| `hufflepuff_virtud_1.mp3` | Veo una lealtad enorme. Tus amigos pueden contar contigo siempre. |
| `hufflepuff_virtud_2.mp3` | Tienes un corazón amable y paciente. Tratas a todos con justicia. |
| `hufflepuff_virtud_3.mp3` | No buscas aplausos: trabajas duro y nunca abandonas lo que empiezas. |
| `hufflepuff_virtud_4.mp3` | Compartes lo que tienes sin esperar nada a cambio. Eso es un tesoro. |
| `hufflepuff_virtud_5.mp3` | Haces sentir a los demás en casa. Es un don muy especial. |
| `hufflepuff_virtud_6.mp3` | Veo humildad y constancia, dos virtudes que valen más que el oro. |
| `hufflepuff_duda_1.mp3` | Aunque también veo una lealtad a toda prueba... hmm. |
| `hufflepuff_duda_2.mp3` | Pero espera... también hay mucha bondad en este corazón. |
| `hufflepuff_duda_3.mp3` | Y por aquí encuentro un corazón enorme... ¡qué difícil decisión! |
| `hufflepuff_anuncio.mp3` | ¡Jáfelpaf! |
| `ravenclaw_virtud_1.mp3` | Qué mente tan curiosa... siempre preguntando el porqué de las cosas. |
| `ravenclaw_virtud_2.mp3` | Veo ingenio y creatividad. Encuentras soluciones donde otros ven problemas. |
| `ravenclaw_virtud_3.mp3` | Te encanta aprender. Cada libro y cada pregunta es una aventura para ti. |
| `ravenclaw_virtud_4.mp3` | Tienes sabiduría para escuchar antes de hablar. Eso es muy poco común. |
| `ravenclaw_virtud_5.mp3` | Tu imaginación no tiene límites. Ves el mundo de una manera única. |
| `ravenclaw_virtud_6.mp3` | Piensas con calma y con claridad, incluso cuando todos se apresuran. |
| `ravenclaw_duda_1.mp3` | Aunque también veo una mente brillante... esto no será tan fácil. |
| `ravenclaw_duda_2.mp3` | Pero espera... también hay mucha curiosidad aquí. Hmm... |
| `ravenclaw_duda_3.mp3` | Y por aquí encuentro una creatividad sorprendente... ¡qué difícil decisión! |
| `ravenclaw_anuncio.mp3` | ¡Réivenclo! |
| `slytherin_virtud_1.mp3` | Veo una determinación de hierro. Cuando te propones algo, lo consigues. |
| `slytherin_virtud_2.mp3` | Tienes madera de líder: los demás te siguen sin que se los pidas. |
| `slytherin_virtud_3.mp3` | Sueñas en grande y trabajas con astucia para alcanzar tus metas. |
| `slytherin_virtud_4.mp3` | Tienes ingenio para salir de cualquier aprieto. Nada te detiene. |
| `slytherin_virtud_5.mp3` | Veo ambición, sí... la buena ambición de querer ser cada día mejor. |
| `slytherin_virtud_6.mp3` | Sabes planear cada paso con cuidado. Eso te llevará muy lejos. |
| `slytherin_duda_1.mp3` | Aunque también veo mucha determinación... hmm, interesante. |
| `slytherin_duda_2.mp3` | Pero espera... también hay madera de líder aquí. |
| `slytherin_duda_3.mp3` | Y por aquí encuentro metas muy grandes... ¡qué difícil decisión! |
| `slytherin_anuncio.mp3` | ¡Slíderin! |
