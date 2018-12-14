package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonReviewList;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.domain.JsonFeed;
import com.noqapp.mobile.domain.JsonFeedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/**
 * hitender
 * 2018-12-07 12:12
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/feed")
public class FeedController {

    private static final Logger LOG = LoggerFactory.getLogger(FeedController.class);

    private ApiHealthService apiHealthService;

    @Autowired
    public FeedController(ApiHealthService apiHealthService) {
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
        value = "/v1/active",
        produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String activeFeed(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput dt
    ) {
        Instant start = Instant.now();
        LOG.info("Feed active for did={} dt={}", did, dt);

        try {
            return getFeedObjs().asJson();
        } catch (Exception e) {
            LOG.error("Feed active reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                "/v1/active",
                "activeFeed",
                ReviewController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.F);
            return new JsonReviewList().asJson();
        } finally {
            apiHealthService.insert(
                "/reviews/{codeQR}",
                "reviews",
                ReviewController.class.getName(),
                Duration.between(start, Instant.now()),
                HealthStatusEnum.G);
        }
    }

    /** This is a temp code. */
    private JsonFeedList getFeedObjs() {
        JsonFeedList jsonFeedList = new JsonFeedList();
        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Being Emotionally Healthy")
            .setImageUrl("https://noqapp.com/imgs/appmages/eyes/single-blue-eye.jpg")
            .setContentId("feed-dec-18")
            .setContent("<p>Being Emotionally Healthy</p>" +
                "<br>" +
                "<h2>1. Think positively.</h2>" +
                "<br>" +
                "<p>It's amazing how much power our minds have over everything in our lives. A simple positive twist on a situation can turn an obstacle into an opportunity. Not only will you have more gusto for life, your immune system can fight off colds and heart disease better Harvard wouldn't lie.</p>" +
                "<br>" +
                "<ul>" +
                "<li>To start this difficult step, focus on gratitude. When you start thinking about the bad thing swirling around you, stop. Cut it out. Think of two things you're grateful for. Eventually, your mind will notice the pattern and stop the negativity before you have to consciously do it.</li>" +
                "</ul>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("12 Ways to Take Care of Your Eyes Everyday")
            .setImageUrl("https://noqapp.com/imgs/appmages/eyes/single-blue-eye.jpg")
            .setContentId("feed-dec-18")
            .setContent("<p>Eye problems&nbsp;can be easily prevented if you practice essential eye care habits everyday. Surprisingly, they are very practical and easy to accomplish yet they tend to be the most neglected.</p>\n" +
                "<p>To maintain your eye health and to keep your vision sharp, here are twelve things that should be part of your daily routine.</p>\n" +
                "<p>&nbsp;</p>\n" +
                "<h2>1. Avoid rubbing your eyes.</h2>\n" +
                "<p>The hands are exposed to a lot of dirt, dust and bacteria, and all of these can be easily transferred to your peepers each time you touch or rub them. So avoid putting your hands to your eyes to prevent infection and irritation. If the habit is so ingrained on you, make an effort to get rid of it as soon as possible.</p><br />\n" +
                "<h2>2. Practice frequent hand washing.</h2>\n" +
                "<p>Wash your hands regularly to keep bacteria at bay and prevent them from getting in contact with your eyes, eyeglasses, and contact lenses.</p><br />\n" +
                "<h2>3. Protect your eyes from the sun.</h2>\n" +
                "<p>Exposure to sunlight and UV rays increases your risk for&nbsp;age-related macular degeneration&nbsp;and may cause cornea sunburn or photokeratitis. So aside from making a fashion statement and adding oomph to your overall look, put on those sunglasses to protect your eyes. If wearing them is not up your alley, UV-protected eyeglasses or contact lenses will do. Putting on caps, visors and hats are also advisable.</p><br />\n" +
                "<h2>4. Stay hydrated.</h2>\n" +
                "<p>Sufficient fluid intake is essential to your body&rsquo;s overall wellbeing, including the eyes. If you&rsquo;re hydrated enough, you prevent your eyes from getting dry and irritated.</p><br />\n" +
                "<h2>5. Don&rsquo;t smoke.</h2>\n" +
                "<p>Smoking makes you more susceptible to age-related macular degeneration and other eye conditions such as&nbsp;cataract. Smoking can also damage the optic nerves, which can have adverse effects on your vision overtime.</p><br />\n" +
                "<h2>6. Keep a balanced diet.</h2>\n" +
                "<p>Beta-carotene, Lutein, Omega-3, Lycopene, and Vitamins C, A, and E are essential for maintaining your eye health. Make sure that your diet is infused with&nbsp;different foods&nbsp;that are rich in those nutrients.</p><br />\n" +
                "<h2>7. Keep proper monitor distance and room lighting.</h2>\n" +
                "<p>Computer monitors should be positioned about an arm&rsquo;s length away from the eyes and 20 degrees below eye level. This keeps your eyes from getting strained. Likewise, make sure that you have sufficient but diffused lighting in your room. Focused and too bright lights may result to glare, and this can put too much stress on the eyes.</p><br />\n" +
                "<h2>8. Observe the 20-20-20 rule.</h2>\n" +
                "<p>If you want to keep your eyes in great shape, you should adhere to the 20-20-20 rule, which states that:</p>\n" +
                "<ul>\n" +
                "<li>Every 20 minutes, look away from your computer monitor and fix your gaze on an object that&rsquo;s 20 feet away from you.</li>\n" +
                "<li>Blink 20 successive times to prevent eye dryness.</li>\n" +
                "<li>Every 20 minutes, get out of your seat and take 20 steps. This is not just good for your vision, but also promotes proper posture and blood circulation throughout the body. Yes, it keeps you from being sedentary too.</li><br />\n" +
                "</ul>\n" +
                "<h2>9. Use the right kind of eye make-up.</h2>\n" +
                "<p>If you wear make-up, choose the brands that work well for you. Steer clear of eye shadows, mascara, and eyeliners that cause an allergic reaction to your eyes. Don&rsquo;t forget to use a make-up remover before going to bed to avoid bacterial build-up from residual make-up left in the eye area. Likewise, clean your make-up brushes regularly, especially those that you use for eye make-up application.</p><br />\n" +
                "<h2>10. Get enough sleep.</h2>\n" +
                "<p>Just like the rest of your body, your eyes need to recharge too, and this happens while you sleep. So make sure that you get sufficient shut-eye each day to keep your eyes revitalized and healthy.</p><br />\n" +
                "<h2>11. Wear the appropriate eye safety gear for different activities.</h2>\n" +
                "<p>No matter what you do, make sure that your eyes are protected. If you&rsquo;re going swimming, wear goggles to avoid exposing your eyes to chlorine. Meanwhile, if you&rsquo;re gardening or attending to a DIY project at home, put on safety glasses to protect your eyes from dust particles, bacteria, and injuries.</p><br />\n" +
                "<h2>12. Keep your surroundings clean.</h2>\n" +
                "<p>Exposure to dirt and dust can irritate the eyes; so make sure that the places you frequent are well maintained and clean. Change you linens and towels regularly and keep your workstation clutter-free.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("7 ways to stay fit and healthy")
            .setImageUrl("https://noqapp.com/imgs/appmages/yoga/yoga.jpg")
            .setContentId("feed-dec-18")
            .setContent("<p>Staying fit and healthy plays an important role in our life. People neglect their health because of the hectic daily schedules but there are little things that you can do each day that will add to being healthy and fit.</p>\n" +
                "<p>Here are some ways to stay fit and healthy:</p>\n" +
                "<br>" +
                "<h3>1. Regular Check-ups</h3>\n" +
                "<p>One should get annual physical check up to make sure everything is as it should be. There is no harm getting regular check ups as it's good for your own body. Do breast or testicular self-exams and get suspicious moles checked out. Getting exams regularly benefits you because if and when something is abnormal, you will get to know about it timely and can consult with your doctor.</p>\n" +
                "<br>" +
                "<h3>2. Get enough sleep</h3>\n" +
                "<p>Getting enough sleep is necessary to stay fit and healthy, many of us do not get enough.</p>\n" +
                "<p>Lack of sleep affects our physical and mental health tremendously. It also affects metabolism, mood, concentration, memory, motor skills, stress hormones and even the immune system and cardiovascular health.</p>\n" +
                "<p>Sleep allows the body to heal, repair and rejuvenate.</p>\n" +
                "<br>" +
                "<h3>3. Exercise</h3>\n" +
                "<p>Exercise is important for being fit and healthy. One should walk for few minutes everyday to stay fit.</p>\n" +
                "<p>It also improves circulation and body awareness and can help combat depression.</p>\n" +
                "<p>Cardiovascular exercise helps to strengthen the heart and lungs, strength training helps to strengthen the muscles and stretching helps to reduce the risk of injury by increasing flexibility.</p>\n" +
                "<br>" +
                "<h3>4. Eat healthy food</h3>\n" +
                "<p>Eat lots of fresh fruits, vegetables,and whole grains to stay healthy and fit. Also include lean sources of protein such as poultry, fish, tofu and beans into your diet.</p>\n" +
                "<p>One should eat a balanced meal and not overeat. Junk foods like burgers, pizza and those that are highly processed and contain artificial sweeteners should be strictly avoided.</p>\n" +
                "<br>" +
                "<h3>5. Do not skip breakfast</h3>\n" +
                "<p>One should have healthy breakfast as it keeps you energetic and fuelled for optimal mental and physical performance. Eating breakfast helps to maintain stable blood sugar levels and a healthy weight because you are less likely to overindulge later in the day.</p>\n" +
                "<br>" +
                "<h3>6. Drink plenty of water</h3>\n" +
                "<p>Drink plenty of water as it helps in keeping our bodies hydrated and to maintain a healthy body. It is the natural cleanser for our organs and digestive system. Water also helps in flushing toxins out through the skin and urine.</p>\n" +
                "<br>" +
                "<h3>7. Do not take stress</h3>\n" +
                "<p>Stress is not good as it harms the body and can cause a myriad of problems, from heart trouble to digestive problems. Exercise, meditation, doing what you love, appropriate boundaries, spirituality, being in nature and enjoyable hobbies helps to alleviate the harmful effects of stress on the body.</p>\n" +
                "<p>Don't overwork and take breaks and surround yourself with people who support you.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Benefits of Yoga: Part-1")
            .setImageUrl("https://noqapp.com/imgs/appmages/yoga/benefits-of-yoga.jpg")
            .setContentId("feed-dec-18")
            .setContent("<h3>1.&nbsp;Improves your flexibility</h3>\n" +
                "<p>Improved&nbsp;flexibility&nbsp;is one of the first and most obvious benefits of yoga. During your first class, you probably won't be able to touch your toes, never mind do a backbend. But if you stick with it, you'll notice a gradual loosening, and eventually, seemingly impossible poses will become possible. You'll also probably notice that aches and pains start to disappear. That's no coincidence. Tight hips can strain the knee joint due to improper alignment of the thigh and shinbones. Tight&nbsp;hamstrings can lead to a flattening of the lumbar spine, which can cause back pain. And inflexibility in muscles and connective tissue, such as fascia and ligaments, can cause poor posture.</p><br />\n" +
                "<h3>2.&nbsp;Builds muscle strength</h3>\n" +
                "<p>Strong muscles do more than look good. They also protect us from conditions like arthritis and&nbsp;back pain, and help prevent falls in elderly people. And when you build strength through yoga, you balance it with flexibility. If you just went to the gym and lifted weights, you might build strength at the expense of flexibility.</p><br />\n" +
                "<h3>3.&nbsp;Perfects your posture</h3>\n" +
                "<p>Your head is like a bowling ball&mdash;big, round, and heavy. When it's balanced directly over an erect spine, it takes much less work for your neck and back muscles to support it. Move it several inches forward, however, and you start to strain those muscles. Hold up that forward-leaning bowling ball for eight or 12 hours a day and it's no wonder you're tired. And fatigue might not be your only problem.&nbsp;Poor posture&nbsp;can cause back, neck, and other muscle and joint problems. As you slump, your body may compensate by flattening the normal inward curves in your neck and lower back. This can cause pain and degenerative arthritis of the spine.</p><br />\n" +
                "<h3>4. Prevents cartilage and joint breakdown&nbsp;</h3>\n" +
                "<p>Each time you practice yoga, you take your joints through their full range of motion. This can help prevent degenerative arthritis or mitigate disability by \"squeezing and soaking\" areas of cartilage that normally aren't used. Joint cartilage is like a sponge; it receives fresh nutrients only when its fluid is squeezed out and a new supply can be soaked up. Without proper sustenance, neglected areas of cartilage can eventually wear out, exposing the underlying bone like worn-out brake pads.</p><br />\n" +
                "<h3>5.&nbsp;Protects your spine</h3>\n" +
                "<p>Spinal disks&mdash;the shock absorbers between the vertebrae that can herniate and compress nerves&mdash;crave movement. That's the only way they get their nutrients. If you've got a well-balanced asana practice with plenty of&nbsp;backbends,&nbsp;forward bends, and&nbsp;twists, you'll help keep your disks supple.</p><br />\n" +
                "<h3>6.&nbsp;Betters your bone health</h3>\n" +
                "<p>It's well documented that weight-bearing exercise strengthens bones and helps ward off osteoporosis. Many postures in yoga require that you lift your own weight. And some, like&nbsp;Downward-&nbsp;and&nbsp;Upward-Facing Dog, help strengthen the arm bones, which are particularly vulnerable to&nbsp;osteoporotic&nbsp;fractures. In an unpublished study conducted at California State University, Los Angeles, yoga practice increased bone density in the vertebrae. Yoga's ability to lower levels of the stress hormone cortisol  may help keep calcium in the bones.</p><br />\n" +
                "<h3>7.&nbsp;Increases your blood flow</h3>\n" +
                "<p>Yoga gets your blood flowing. More specifically, the relaxation exercises you learn in yoga can help your circulation, especially in your hands and feet. Yoga also gets more oxygen to your cells, which function better as a result. Twisting poses are thought to wring out venous blood from internal organs and allow oxygenated blood to flow in once the twist is released. Inverted poses, such as Headstand,&nbsp;Handstand, and Shoulderstand, encourage venous blood from the legs and pelvis to flow back to the&nbsp;heart, where it can be pumped to the lungs to be freshly oxygenated. This can help if you have swelling in your legs from heart or kidney problems. Yoga also boosts levels of hemoglobin and red blood cells, which carry oxygen to the tissues. And it thins the blood by making platelets less sticky and by cutting the level of clot-promoting proteins in the blood. This can lead to a decrease in heart attacks and strokes since blood clots are often the cause of these killers.</p><br />\n" +
                "<h3>8.&nbsp;Drains your lymphs and boosts immunity</h3>\n" +
                "<p>When you contract and stretch muscles, move organs around, and come in and out of yoga postures, you increase the drainage of lymph (a viscous fluid rich in immune cells). This helps the lymphatic system fight infection, destroy cancerous cells, and dispose of the toxic waste products of cellular functioning.</p><br />\n" +
                "<h3>9.&nbsp;Ups&nbsp;your heart rate</h3>\n" +
                "<p>When you regularly get your heart rate into the aerobic range, you lower your risk of heart attack and can relieve&nbsp;depression. While not all yoga is aerobic, if you do it vigorously or take flow or Ashtanga classes, it can boost your heart rate into the aerobic range. But even yoga exercises that don't get your heart rate up that high can improve cardiovascular conditioning. Studies have found that yoga practice lowers the resting heart rate, increases endurance, and can improve your maximum uptake of oxygen during exercise&mdash;all reflections of improved aerobic conditioning. One study found that subjects who were taught only pranayama could do more exercise with less oxygen.</p><br />\n" +
                "<h3>10. Drops your blood pressure</h3>\n" +
                "<p>If you've got&nbsp;high blood pressure, you might benefit from yoga. Two studies of people with hypertension, published in the British medical journal&nbsp;<em>The Lancet</em>, compared the effects of&nbsp;Savasana (Corpse Pose)&nbsp;with simply lying on a couch. After three months, Savasana was associated with a 26-point drop in systolic blood pressure (the top number) and a 15-point drop in diastolic blood pressure (the bottom number&mdash;and the higher the initial blood pressure, the bigger the drop.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Benefits of Yoga: Part-2")
            .setImageUrl("https://noqapp.com/imgs/appmages/yoga/benefits-of-yoga.jpg")
            .setContentId("feed-dec-18")
            .setContent("<h3>11.&nbsp;Regulates your adrenal glands</h3>\n" +
                "<p>Yoga lowers cortisol levels. If that doesn't sound like much, consider this. Normally, the adrenal glands secrete cortisol in response to an acute crisis, which temporarily boosts immune function. If your cortisol levels stay high even after the crisis, they can compromise the immune system. Temporary boosts of cortisol help with long-term memory, but chronically high levels undermine memory and may lead to permanent changes in the brain. Additionally, excessive cortisol has been linked with major depression, osteoporosis (it extracts calcium and other minerals from bones and interferes with the laying down of new bone), high blood pressure, and insulin resistance. In rats, high cortisol levels lead to what researchers call \"food-seeking behavior\" (the kind that drives you to eat when you're upset, angry, or stressed). The body takes those extra calories and distributes them as fat in the abdomen, contributing to weight gain and the risk of diabetes and heart attack.</p><br />\n" +
                "<h3>12.&nbsp;Makes you happier</h3>\n" +
                "<p>Feeling sad? Sit in Lotus. Better yet, rise up into a backbend or soar royally into King Dancer Pose. While it's not as simple as that, one study found that a consistent yoga practice improved depression and led to a significant increase in serotonin levels and a decrease in the levels of monoamine oxidase (an enzyme that breaks down neurotransmitters) and cortisol. At the University of Wisconsin, Richard Davidson, Ph.D., found that the left prefrontal cortex showed heightened activity in meditators, a finding that has been correlated with greater levels of happiness and better immune function. More dramatic left-sided activation was found in dedicated, long-term practitioners.</p><br />\n" +
                "<h3>13. Founds a healthy lifestyle</h3>\n" +
                "<p>Move more, eat less&mdash;that's the adage of many a dieter. Yoga can help on both fronts. A regular practice gets you moving and burns calories, and the spiritual and emotional dimensions of your practice may encourage you to address any eating and weight problems on a deeper level. Yoga may also inspire you to become a more conscious eater.</p><br />\n" +
                "<h3>14.&nbsp;Lowers blood sugar</h3>\n" +
                "<p>Yoga lowers blood sugar and LDL (\"bad\") cholesterol and boosts HDL (\"good\") cholesterol. In people with diabetes, yoga has been found to lower blood sugar in several ways: by lowering cortisol and adrenaline levels, encouraging weight loss, and improving sensitivity to the effects of insulin. Get your blood sugar levels down, and you decrease your risk of diabetic complications such as heart attack, kidney failure, and blindness.</p><br />\n" +
                "<h3>15.&nbsp;Helps you focus</h3>\n" +
                "<p>An important component of yoga is focusing on the present. Studies have found that regular yoga practice improves coordination, reaction time, memory, and even IQ scores. People who practice Transcendental Meditation demonstrate the ability to solve problems and acquire and recall information better&mdash;probably because they're less distracted by their thoughts, which can play over and over like an endless tape loop.</p><br />\n" +
                "<h3>16.&nbsp;Relaxes your system&nbsp;</h3>\n" +
                "<p>Yoga encourages you to relax, slow your breath, and focus on the present, shifting the balance from the sympathetic nervous system (or the fight-or-flight response) to the parasympathetic nervous system. The latter is calming and restorative; it lowers breathing and heart rates, decreases blood pressure, and increases blood flow to the intestines and reproductive organs&mdash;comprising what Herbert Benson, M.D., calls the relaxation response.</p><br />\n" +
                "<h3>17.&nbsp;Improves your balance</h3>\n" +
                "<p>Regularly practicing yoga increases proprioception (the ability to feel what your body is doing and where it is in space) and improves balance. People with bad posture or dysfunctional movement patterns usually have poor proprioception, which has been linked to knee problems and&nbsp;back pain. Better balance could mean fewer falls. For the elderly, this translates into more independence and delayed admission to a nursing home or never entering one at all. For the rest of us, postures like Tree Pose can make us feel less wobbly on and off the mat.</p><br />\n" +
                "<h3>18.&nbsp;Maintains your nervous system</h3>\n" +
                "<p>Some advanced yogis can control their bodies in extraordinary ways, many of which are mediated by the nervous system. Scientists have monitored yogis who could induce unusual heart rhythms, generate specific brain-wave patterns, and, using a meditation technique, raise the temperature of their hands by 15 degrees Fahrenheit. If they can use yoga to do that, perhaps you could learn to improve blood flow to your pelvis if you're trying to get pregnant or induce relaxation when you're having trouble falling asleep.</p><br />\n" +
                "<h3>19.&nbsp;Releases tension in your limbs</h3>\n" +
                "<p>Do you ever notice yourself holding the telephone or a steering wheel with a death grip or scrunching your face when staring at a computer screen? These unconscious habits can lead to chronic tension, muscle fatigue, and soreness in the wrists, arms, shoulders, neck, and face, which can increase stress and worsen your mood. As you practice yoga, you begin to notice where you hold tension: It might be in your tongue, your eyes, or the muscles of your face and neck. If you simply tune in, you may be able to release some tension in the tongue and eyes. With bigger muscles like the quadriceps, trapezius, and buttocks, it may take years of practice to learn how to relax them.</p><br />\n" +
                "<h3>20.&nbsp;Helps you sleep deeper</h3>\n" +
                "<p>Stimulation is good, but too much of it taxes the nervous system. Yoga can provide relief from the hustle and bustle of modern life. Restorative asana,&nbsp;yoga nidra&nbsp;(a form of guided relaxation), Savasana, pranayama, and meditation encourage&nbsp;pratyahara, a turning inward of the senses, which provides downtime for the nervous system. Another by-product of a regular yoga practice, studies suggest, is better sleep&mdash;which means you'll be less tired and stressed and less likely to have accidents.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Benefits of Yoga: Part-3")
            .setImageUrl("https://noqapp.com/imgs/appmages/yoga/benefits-of-yoga.jpg")
            .setContentId("feed-dec-18")
            .setContent("<h3>21.&nbsp;Boosts your immune system functionality</h3>\n" +
                "<p>Asana and pranayama probably improve immune function, but, so far, meditation has the strongest scientific support in this area. It appears to have a beneficial effect on the functioning of the immune system, boosting it when needed (for example, raising antibody levels in response to a vaccine) and lowering it when needed (for instance, mitigating an inappropriately aggressive immune function in an autoimmune disease like psoriasis).</p><br />\n" +
                "<h3>22.&nbsp;Gives your lungs room to breathe</h3>\n" +
                "<p>Yogis tend to take fewer breaths of greater volume, which is both calming and more efficient. A 1998 study published in The Lancet taught a yogic technique known as \"complete breathing\" to people with lung problems due to congestive heart failure. After one month, their average respiratory rate decreased from 13.4 breaths per minute to 7.6. Meanwhile, their exercise capacity increased significantly, as did the oxygen saturation of their blood. In addition, yoga has been shown to improve various measures of&nbsp;lung function, including the maximum volume of the breath and the efficiency of the exhalation.</p><br />\n" +
                "<p>Yoga also promotes breathing through the nose, which filters the air, warms it (cold, dry air is more likely to trigger an asthma attack in people who are sensitive), and humidifies it, removing pollen and dirt and other things you'd rather not take into your lungs.</p><br />\n" +
                "<h3>23.&nbsp;Prevents IBS and other digestive problems</h3>\n" +
                "<p>Ulcers, irritable bowel syndrome, constipation&mdash;all of these can be exacerbated by stress. So if you stress less, you'll suffer less. Yoga, like any physical exercise, can ease constipation&mdash;and theoretically lower the risk of colon cancer&mdash;because moving the body facilitates more rapid transport of food and waste products through the bowels. And, although it has not been studied scientifically, yogis suspect that&nbsp;twisting poses&nbsp;may be beneficial in getting waste to move through the system.</p><br />\n" +
                "<h3>24.&nbsp;Gives you peace of mind</h3>\n" +
                "<p>Yoga quells the fluctuations of the mind, according to&nbsp;Patanjali's Yoga Sutra. In other words, it slows down the mental loops of frustration, regret, anger, fear, and desire that can cause stress. And since stress is implicated in so many health problems&mdash;from migraines and insomnia to lupus, MS, eczema, high blood pressure, and heart attacks&mdash;if you learn to quiet your mind, you'll be likely to live longer and healthier.</p><br />\n" +
                "<h3>25.&nbsp;Increases your self-esteem&nbsp;</h3>\n" +
                "<p>Many of us suffer from chronic low self-esteem. If you handle this negatively&mdash;take drugs, overeat, work too hard, sleep around&mdash;you may pay the price in poorer health physically, mentally, and spiritually. If you take a positive approach and practice yoga, you'll sense, initially in brief glimpses and later in more sustained views, that you're worthwhile or, as yogic philosophy teaches, that you are a manifestation of the Divine. If you practice regularly with an intention of self-examination and betterment&mdash;not just as a substitute for an aerobics class&mdash;you can access a different side of yourself. You'll experience feelings of gratitude, empathy, and forgiveness, as well as a sense that you're part of something bigger. While better health is not the goal of&nbsp;spirituality, it's often a by-product, as documented by repeated scientific studies.</p><br />\n" +
                "<h3>26.&nbsp;Eases your pain</h3>\n" +
                "<p>Yoga can ease your pain. According to several studies, asana, meditation, or a combination of the two, reduced pain in people with arthritis, back pain, fibromyalgia,&nbsp;carpal tunnel syndrome, and other chronic conditions. When you relieve your pain, your mood improves, you're more inclined to be active, and you don't need as much medication.</p><br />\n" +
                "<h3>27.&nbsp;Gives you inner strength</h3>\n" +
                "<p>Yoga can help you make changes in your life. In fact, that might be its greatest strength. Tapas, the Sanskrit word for \"heat,\" is the fire, the discipline that fuels yoga practice and that regular practice builds. The tapas you develop can be extended to the rest of your life to overcome inertia and change dysfunctional habits. You may find that without making a particular effort to change things, you start to eat better, exercise more, or finally quit smoking after years of failed attempts.</p><br />\n" +
                "<h3>28.&nbsp;Connects you with guidance&nbsp;</h3>\n" +
                "<p>Good yoga teachers can do wonders for your health. Exceptional ones do more than guide you through the postures. They can adjust your posture, gauge when you should go deeper in poses or back off, deliver hard truths with compassion, help you relax, and enhance and personalize your practice. A respectful relationship with a teacher goes a long way toward promoting your health.</p><br />\n" +
                "<h3>29.&nbsp;Helps keep&nbsp;you drug free</h3>\n" +
                "<p>If your medicine cabinet looks like a pharmacy, maybe it's time to try yoga. Studies of people with asthma, high blood pressure, Type II diabetes (formerly called adult-onset diabetes), and obsessive-compulsive disorder have shown that yoga helped them lower their dosage of medications and sometimes get off them entirely. The benefits of taking fewer drugs? You'll spend less money, and you're less likely to suffer side effects and risk dangerous drug interactions.</p><br />\n" +
                "<h3>30.&nbsp;Builds awareness for transformation</h3>\n" +
                "<p>Yoga and meditation build awareness. And the more aware you are, the easier it is to break free of destructive emotions like anger. Studies suggest that chronic anger and hostility are as strongly linked to heart attacks as are smoking, diabetes, and elevated cholesterol. Yoga appears to reduce anger by increasing feelings of compassion and interconnection and by calming the nervous system and the mind. It also increases your ability to step back from the drama of your own life, to remain steady in the face of bad news or unsettling events. You can still react quickly when you need to&mdash;and there's evidence that yoga speeds reaction time&mdash;but you can take that split second to choose a more thoughtful approach, reducing suffering for yourself and others.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Special Skin Care for Summer")
            .setAuthor("Dr. Sneh Thadani")
            .setAuthorThumbnail("http://www.ssdhospital.in/wp-content/uploads/2017/03/Dr-Sneh-Thadani-200x300-1-e1490009312556.jpeg")
            .setProfession("MBBS DNB DERMATOLOGY\n" +
                "Head of Dermatology Department, SSD Hospital")
            .setWebProfileId("")
            .setImageUrl("https://noqapp.com/imgs/appmages/skin/skin.jpg")
            .setContentId("feed-dec-18")
            .setContent("<p>Summers can be severe. The skin feels the heat too. Proper care needs to be taken for it to radiate even when the temperatures rise. Dr. Thadani guides us by giving us some useful pointers. Read on..</p><br />\n" +
                "<p><strong>Effects of the summer heat and humidity on the skin</strong></p>\n" +
                "<p>Excess heat increases sweat production and the humidity hinders its ability to evaporate away from the skin. This means more oil is available to clog pores which can exacerbate acne. Also, extreme heat and humidity can facilitate bacterial infections such as impetigo and fungal infections like athlete&rsquo;s footor intertrigo, which are rashes seen in body folds. This is that time of the year when a lot of patients who battle those conditions become very uncomfortable.</p><br />\n" +
                "<p><strong>Essentials of skin care</strong></p>\n" +
                "<p>Nothing is more important than wearing sunscreen (ideally, spf 30) every day. Many dermatologists even recommend layering sunscreens with a chemical blocker first followed by a physical blocking sunscreen which contains zinc and/or titanium. This way the sun&rsquo;s rays are primarily reflected away, but what does get through is then absorbed by the chemical sun-screen beneath.</p><br />\n" +
                "<p><strong>Acne breakouts:</strong></p>\n" +
                "<p>When sweat mixes with bacteria and oils on your skin, it can clog your pores. If you have acne-prone skin, this often means breakouts. Dermatologists recommend the following to help prevent acne:</p><br />\n" +
                "<p>&bull; Blot sweat from your skin with a clean towel or cloth. wiping sweat off can irritate your skin, which can lead to a breakout.<br />&bull; Wash sweaty clothes, headbands, towels, and hats before wearing them again.<br />&bull; Use non-comedogenic products on your face, neck, back, and chest. The label may also say &ldquo;oil free&rdquo; or &ldquo;won&rsquo;t clog pores.&rdquo;</p><br />\n" +
                "<p><strong>Dry, Irritated skin</strong></p>\n" +
                "<p>When outdoor air is hot and humid, you can still have dry irritated skin. The biggest culprits are spending time in the sun, pool, and air-conditioning. If your skin starts to feel dry and irritated despite the humidity, try these tips:</p><br />\n" +
                "<p>&bull; Shower and shampoo immediately after getting out of the pool, using fresh, clean water and a mild cleanser or body wash made for swimmers.<br />&bull; Apply sunscreen before going outdoors, using one that offers broad-spectrum protection, spf 30+, and water resistance.&bull; Use a mild cleanser to wash your skin. soaps and body washes labeled &ldquo;antibacterial&rdquo; or &ldquo;deodorant&rdquo; can dry your skin.<br />&bull; Take showers and baths in warm rather than hot water.&bull; Slather on a fragrance-free moisturizer after every shower and bath. moisturizer works by trapping water in your skin, so you&rsquo;ll need to apply it within 5 minutes of taking a shower or bath.<br />&bull; Carry moisturizer with you, so you can apply it after washing your hands and when your skin feels dry.<br />&bull; Turn up the thermostat if the air conditioning makes your home too dry.<br />&bull; Steer clear of thick and heavy foundations.</p><br />\n" +
                "<p><strong>Prickly heat (or heat rash)</strong></p>\n" +
                "<p>Blocked sweat glands cause this because the sweat cannot get out, It builds up under your skin, causing a rash and tiny, itchy bumps. When the bumps burst and release sweat, many people feel a prickly sensation on their skin.</p><br />\n" +
                "<p>Anything you can do to stop sweating profusely will help reduce your risk. Tips that dermatologists offer to their patients to help them sweat less and thereby lessen their risk of getting prickly heat include:</p><br />\n" +
                "<p>&bull; Wear light-weight, loose-fitting clothes made of cotton.<br />&bull; Exercise outdoors during the coolest parts of the day or move your workout indoors where you can be in air-conditioning.<br />&bull; Try to keep your skin cool by using fans, cool showers, and air-conditioning when possible.</p><br />\n" +
                "<p><strong>Sun allergy:</strong>&nbsp;You can develop hives (an allergic skin reaction) when you&rsquo;re in the sun if you:</p>\n" +
                "<p>&bull; Take certain medications<br />&bull; Have a sun sensitivity (usually runs in the family) If you have an allergic reaction to the sun, you&rsquo;ll see red, scaly, and extremely itchy bumps on some (or all) bare skin. Some people also get blisters. To prevent an allergic skin reaction:<br />&bull; Check your medication container to find out if it can cause an allergic reaction when you go out in the sun. Medications that can cause an allergic sun reaction include ketoprofen (found in some pain meds) and these antibiotics &mdash; tetracycline, doxycycline, and minocycline. If the medicine can cause a reaction, stay out of the sun.<br />&bull; Protect your skin from the sun. You can do this by seeking shade, wearing sun-protective clothes, and applying sunscreen that offers broad spectrum protection, water resistance, and an SPF of 30 or more.</p>\n"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Healing Techniques for Acne Scars")
            .setAuthor("Dr. Sneh Thadani")
            .setAuthorThumbnail("http://www.ssdhospital.in/wp-content/uploads/2017/03/Dr-Sneh-Thadani-200x300-1-e1490009312556.jpeg")
            .setProfession("MBBS DNB DERMATOLOGY\n" +
                "Head of Dermatology Department, SSD Hospital")
            .setWebProfileId("")
            .setImageUrl("http://www.ssdhospital.in/wp-content/uploads/2017/09/Healing-techniques-for-acne-2.png")
            .setContentId("feed-dec-18")
            .setContent("<p>If acne scars bother you, there are safe and effective treatments available. Treatment can diminish acne scars that cause depressions in the skin. Treatment can also safely reduce raised acne scars. There are several treatments possible in today&rsquo;s time and technology. Treatments include laser treatments,minor skin surgeries, chemical peels, and fillers. As we age, acne scars often become more noticeable because our skin loses collagen. The key to effective treatment is to select the best one for each scar type.</p>\n" +
                "<br>" +
                "<p>Before getting treatment for acne scars, it is important to clear your acne. New acne breakouts can lead to new acne scars. Having acne also means that your skin is inflamed. Inflammation reduces the effectiveness of treatment for acne scars.</p>" +
                "<br>" +
                "<p>Treatment for acne scars At SSD Hospital, we use procedures to minimize acne scars which are determined by your health care provider based on:</p>" +
                "<br>" +
                "<p>&bull; Your age, overall health, and medical history<br />&bull; Severity of the scar<br />&bull; Type of scar<br />&bull; Your tolerance for specific medications, procedures, or therapies<br />&bull; Your opinion or preference</p>" +
                "<br>" +
                "<p>&bull;&nbsp;<strong>&nbsp;Dermabrasion </strong>&nbsp;&ndash; may be used to minimize small scars, minor skin surface irregularities, surgical scars, and acne scars. As the name implies, dermabrasion involves removing the top layers of skin with an electrical machine that abrades the skin. As the skin heals from the procedure, the surface appears smoother and fresher. You will typically need 5 to 7 days at home to recover. Microdermabrasion is a less invasive option typically performed in a series of treatments that do not require the same downtime.</p>" +
                "<br>" +
                "<p>&bull;&nbsp;<strong>&nbsp;Chemical peels</strong>&nbsp;&ndash; Chemical peels are often used to minimize sun-damaged skin, irregular pigment, and superficial scars. The top layer of skin is removed with a chemical application to the skin. By removing the top layer, the skin regenerates, often improving its appearance.</p>" +
                "<br>" +
                "<p>&bull;&nbsp;<strong>Dermal filler injections</strong>&nbsp;-These are injected beneath the skin to replace the body&rsquo;s natural collagen that has been lost. Injectable dermal fillers are generally used to treat wrinkles, scars, and facial lines. Most fillers are temporary, lasting about 1 year.</p>" +
                "<br>" +
                "<p>&bull;&nbsp;<strong>Laser resurfacing and light therapies</strong>&nbsp;&ndash; Laser resurfacing uses high-energy light to burn away damaged skin. Laser resurfacing may be used to minimize wrinkles and fine scars. A pulsed dye laser may be used to specifically treat raised acne scars. Additionally, for lighter-skinned people, intense pulsed light may be another treatment option.</p>" +
                "<br>" +
                "<p>&bull;&nbsp;<strong>Injections</strong>&nbsp;&ndash; Commonly referred to as intralesional injections. These involve injecting a medication, usually a steroid, directly into a<br />raised scar, causing it to flatten and soften.</p>" +
                "<br>" +
                "<p>&bull;&nbsp;<strong>Cryotherapy</strong>&nbsp;&ndash; The purpose of this treatment is to freeze scar tissue, causing it to die and eventually fall off. Sometimes, this is used in combination with intralesional injections. One potential risk of cryotherapy, however, is lightening the skin, causing the treated area to be lighter than the surrounding skin.</p>" +
                "<br>" +
                "<p>&bull;&nbsp;<strong>Topical creams</strong>&nbsp;&ndash; Creams with retinoids or silicone can also help reduce the appearance of scars. Microneedling-RF with radio frequency waves is an other technology that can work for very deep and pitted acne scars. These treatments then trigger the body&rsquo;s natural process, stimulating<br />growth of new, healthy tissue, which results in smooth, refreshed and scar free skin.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Diet During Pregnancy")
            .setAuthor("Dr. Shweta Upadhyay")
            .setAuthorThumbnail("http://www.ssdhospital.in/wp-content/uploads/2016/12/dr-shweta-upadhyay-294x300.jpg")
            .setProfession("Consultant Gynaecologist, SSD Hospital")
            .setWebProfileId("")
            .setImageUrl("https://noqapp.com/imgs/appmages/Pregnant_Woman_Holding_Apple.jpg")
            .setContentId("feed-dec-18")
            .setContent("<h3><strong>What to eat and what not to eat?</strong></h3>" +
                "<br>" +
                "<p>Pregnancy brings in a lot of emotions. Every one you meet has a piece of advice to give you regarding what you should do and what you should eat. This brings in even more confusion. This Indian diet plan for pregnancy will help you clear the air a bit.</p>" +
                "<br>" +
                "<p>What you eat will play a crucial role in your unborn baby&rsquo;s growth and development. The key to good nutrition during pregnancy is balance. It is important to eat right types of food in right quantity.<br />Here are a few food groups to include in your everyday diet.</p>" +
                "<br>" +
                "<p><strong>Milk and Milk Products:</strong>&nbsp;This group is a major source of protein, calcium, phosphorus and vitamins. Dairy products are one of the best sources of calcium and your body needs a lot of it during pregnancy. Cheese, milk, yogurt and paneer are excellent foods for pregnancy.</p>" +
                "<br>" +
                "<p><strong>Meat, Poultry, Fish,&nbsp;</strong>Eggs<strong>&nbsp;and Nuts:</strong>&nbsp;These foods provide protein, iron and zinc. There is an increased demand for protein particularly in the second half of pregnancy.</p>" +
                "<br>" +
                "<p><strong>Pulses, Dals, Cereals,&nbsp;</strong>Nuts<strong>&nbsp;and Wholegrains:</strong>&nbsp;If you are not a meat eater, include Pulses, Dals, Cereals, Nuts and Wholegrains to make up for body&rsquo;s extra requirement of protein.</p>" +
                "<br>" +
                "<p>If you are a vegetarian, you will need about 45 gms of Nuts and 2/3 rd cups of pulses each day. This group provide complex carbohydrates (starches), an important source of energy, in addition to vitamins, minerals and fibre.</p>" +
                "<br>" +
                "<p><strong>Fruits:</strong>&nbsp;This group provides Important nutrition during pregnancy like vitamins A and C, potassium and fibre. Fruits such as oranges, grapefruit, melons and berries are the best sources of Vitamin C. It is advisable to eat three or more servings of fruit a day.</p>" +
                "<br>" +
                "<p><strong>Vegetables:</strong>&nbsp;One should have raw, leafy vegetables like spinach (palak) and other vegetables like carrots (gajar), sweet potatoes (shakarkand), corn (makka), peas (matar) and potatoes. These foods contain vitamins A and C, folate, and minerals such as iron and magnesium. They are also low in fat and contain fibre, which helps to alleviate constipation.</p>" +
                "<br>" +
                "<p><strong>Fats, Oils and Sweets:</strong>&nbsp;Use sparingly, since these products contain calories, but few vitamins or minerals. Fats should not make up more than 30 percent of your daily calories. Try to select low-fat foods.</p>" +
                "<br>" +
                "<p><strong>Iron-Rich Foods:</strong>&nbsp;Pregnant women need 27 milligrams of iron a day, which is double the amount needed by women who are not expecting. Getting too little iron during pregnancy can lead to anaemia, a condition resulting in weakness and an increased risk of infections.</p>" +
                "<br>" +
                "<p>For better absorption of the mineral, include a good source of vitamin C at the same meal when eating iron-rich foods. Food sources for iron are meat, raisins, prunes, beans, poultry, soy and spinach. Foods rich in vitamin C &ndash; citrus fruits, potatoes and broccoli.</p>" +
                "<br>" +
                "<p><strong>Folic Acid:</strong>&nbsp;Folic acid, also known as folate when found in foods, is a B vitamin that is crucial in helping to prevent birth defects in the baby&rsquo;s brain and spine, known as neural tube defects. It may be hard to get the recommended amount of folic acid from diet alone. It is recommended that women who are trying to have a baby should take 400 micrograms of folic acid per day for at least&nbsp;one month before becoming pregnant. Food sources for folic acid are leafy green vegetables, fortified or enriched cereals, breads and pastas.</p>" +
                "<br>" +
                "<p><strong>Calcium:</strong>&nbsp;is a mineral used to build a baby&rsquo;s bones and teeth. If a pregnant woman does not consume enough calcium, the mineral will be drawn from the mother&rsquo;s stores in her bones and given to the baby to meet the extra demands of pregnancy. Many dairy products are also fortified with vitamin D, another nutrient that works with calcium to develop a baby&rsquo;s bones and teeth. Pregnant women needs 1,000 milligrams of calcium a day. Food sources for milk, curd, cheese, fruits and vegetables, and fish.</p>" +
                "<br>" +
                "<p><strong>Protein:</strong>&nbsp;More protein is needed during pregnancy, but most women don&rsquo;t have problems getting enough of these foods in their diets. Protein is described as &ldquo;a builder nutrient,&rdquo; because it helps to build important organs in the baby. Food sources for Protein are pulses, milk &amp; milk products, soya, meat, fish, beans and peas, eggs, nuts.</p>" +
                "<br>" +
                "<p><strong>Foods to limit:</strong>&nbsp;Consumption of Caffeine, sugary drinks, fast food and certain categories of fish containing high levels of mercury should be limited.</p>" +
                "<br>" +
                "<p><strong>Foods to avoid:</strong>&nbsp;Consumption of alcohol, smoking tobacco and unpasteurised food, for ex. raw meat, raw milk, should be avoided.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Pregnancy Diet Misconceptions")
            .setAuthor("Dr. Shweta Upadhyay")
            .setAuthorThumbnail("http://www.ssdhospital.in/wp-content/uploads/2016/12/dr-shweta-upadhyay-294x300.jpg")
            .setProfession("Consultant Gynaecologist, SSD Hospital")
            .setWebProfileId("")
            .setImageUrl("https://noqapp.com/imgs/appmages/Pregnant_Woman_Holding_Apple.jpg")
            .setContentId("feed-dec-18")
            .setContent("<p><strong>Morning sickness:</strong>&nbsp;When a mother-to-be is experiencing morning sickness, the biggest mistake she can make is thinking that if she doesn&rsquo;t eat, she&rsquo;ll feel better. And the phrase Morning sickness is definitely not happening only in the morning, It&rsquo;s any time of day. It is advisable to eat small amounts of foods that don&rsquo;t have an odour, since smells can also upset the stomach.</p>" +
                "<br>" +
                "<p><strong>Food cravings:</strong>&nbsp;It is common for women to develop a sudden urge or a strong dislike for a food during pregnancy. Some common cravings are for sweets, salty or spicy foods. This food craving is a body&rsquo;s way of saying it needs a specific nutrient, such as more protein or additional liquids to quench a thirst, rather than a particular food.</p>" +
                "<br>" +
                "<p><strong>Eating for two:</strong>&nbsp;When people say that a pregnant woman should &ldquo;eat for two&rdquo;, it doesn&rsquo;t mean she needs to consume twice as much food or double her calories. It is advisable to add just 300 calories to her usual dietary intake.</p>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Weight Gain During Pregnancy")
            .setAuthor("Dr. Shweta Upadhyay")
            .setAuthorThumbnail("http://www.ssdhospital.in/wp-content/uploads/2016/12/dr-shweta-upadhyay-294x300.jpg")
            .setProfession("Consultant Gynaecologist, SSD Hospital")
            .setWebProfileId("")
            .setImageUrl("https://noqapp.com/imgs/appmages/preg.jpg")
            .setContentId("feed-dec-18")
            .setContent("<p>Weight gain during pregnancy is a continuous process over the nine months. It&rsquo;s hard to measure where pregnancy weight is going whether the KGs are going to a woman&rsquo;s body fat, baby weight or fluid gains.<br />When it comes to pregnancy weight gain, women should look at the big picture: During regular prenatal check-ups, focus on that the baby is growing normally rather than worrying about the number on a scale.</p>" +
                "<br>" +
                "<p>The total number of calories needed per day during pregnancy depends on a woman&rsquo;s height, her weight before becoming pregnant, and how active she is on a daily basis. In general, underweight women need more calories during pregnancy; overweight and obese women need fewer of them.</p>" +
                "<br>" +
                "<p>Various global guidelines for total weight gain during a full-term pregnancy recommend that:</p>" +
                "<ul>" +
                "<li>&nbsp;Underweight women, who have a Body Mass Index (BMI) below 18.5, should gain 12 to 18 Kgs</li>" +
                "<li>&nbsp;Normal weight women, who have a BMI of 18.5 to 24.9, should gain 11. to 16 Kgs</li>" +
                "<li>&nbsp;Overweight women, who have a BMI of 25.0 to 29.9, should gain 7 to 11 Kgs).</li>" +
                "<li>&nbsp;Obese women, who have a BMI of 30.0 and above, should gain 5 to 9 Kgs</li>" +
                "</ul>"));

        jsonFeedList.addJsonFeed(new JsonFeed()
            .setTitle("Exercise During Pregnancy")
            .setAuthor("Dr. Shweta Upadhyay")
            .setAuthorThumbnail("http://www.ssdhospital.in/wp-content/uploads/2016/12/dr-shweta-upadhyay-294x300.jpg")
            .setProfession("Consultant Gynaecologist, SSD Hospital")
            .setWebProfileId("")
            .setImageUrl("http://www.ssdhospital.in/wp-content/uploads/2017/01/183937263_wide.jpg")
            .setContentId("feed-dec-18")
            .setContent("<p>Maintaining a regular exercise routine throughout your pregnancy can help you stay healthy and feel your best. Regular exercise during pregnancy can improve your posture and decrease some common discomforts such as backaches and fatigue. There is evidence that physical activity may prevent gestational diabetes (diabetes that develops during pregnancy), relieve stress, and build more stamina needed for labour and delivery.</p>" +
                "<br>" +
                "<p>Doctors generally recommend 30 minutes or more of moderate exercise per day, unless you have a medical or pregnancy complication.</p>" +
                "<br>" +
                "<p>Most exercises are safe to perform during pregnancy, as long as you exercise with caution and do not overdo it. The safest and most productive activities are swimming, brisk walking, indoor stationary cycling, step or elliptical machines and Yoga.</p>" +
                "<br>" +
                "<p>However, if you have a medical problem, such as asthma or heart disease, or diabetes, exercise may not be advisable. Exercise may also be harmful if you have some pregnancy-related conditions such as bleeding, spotting, threatened or recurrent miscarriage, previous premature births or history of early labour, etc. It is always advisable to discuss with your doctor before beginning any specific exercise program.</p>"));

//        feedObjs.add(new FeedObj().setTitle("Stages of Pregnancy").
//                setImageUrl("http://www.ssdhospital.in/wp-content/uploads/2017/01/15977659_1273843289338522_6523698174661880087_n-e1489668918984.jpg")
//                .setContent("<p>In India, getting pregnant and giving birth are most celebrated milestones in a woman&rsquo;s life.<br />Pregnancy, specifically in India, is very important event due to its connection with our history, culture,religious beliefs and mythology.</p>\n" +
//                        "<p>However, Pregnancy, being one the most scientifically complicated process in human life, has its own sets of questions and concerns in the mind of every woman. This article is to give an overview of Pregnancy lifecycle and address the day to day concerns.</p><br />\n" +
//                        "<h2><strong>Stages of Pregnancy:</strong></h2>\n" +
//                        "<p>What really happens during the nine golden months in women&rsquo;s life?</p>\n" +
//                        "<p>A normal pregnancy lasts for 40 weeks from the first day of your last menstrual period (LMP) to the birth of the baby. The 9 months of pregnancy are divided into three stages, called trimesters:</p>\n" +
//                        "<ul>\n" +
//                        "<li>First trimester &ndash; from conception to 12th week</li>\n" +
//                        "<li>Second trimester &ndash; from 13th week to 28th week</li>\n" +
//                        "<li>Third trimester &ndash; from 29th week to child birth</li>\n" +
//                        "</ul><br />\n" +
//                        "<p><strong>First trimester:&nbsp;</strong>(from conception to 12th week)</p>\n" +
//                        "<p><strong>Early Changes in a Woman&rsquo;s Body</strong>&nbsp;&ndash; A missed period would most likely be the first noticeable thing in a pregnant woman. A woman will experience a lot of symptoms during her first trimester as she adjusts to the hormonal changes of pregnancy, which affect nearly every organ in her body. The pregnancy may not be showing much on the outside of her body, but inside many changes are taking place. Some of these noticeable physical and emotional changes during the first trimester are as follows.</p>\n" +
//                        "<ul>\n" +
//                        "<li>Extreme fatigue or weakness</li>\n" +
//                        "<li>Tender and swollen breasts and changes in the Nipples.</li>\n" +
//                        "<li>Nausea with or without vomiting (morning sickness)</li>\n" +
//                        "<li>Cravings or dislike to certain foods, loss of appetite</li>\n" +
//                        "<li>Headache</li>\n" +
//                        "<li>Heartburn</li>\n" +
//                        "<li>Constipation</li>\n" +
//                        "<li>Frequent urination</li>\n" +
//                        "<li>Weight gain or loss</li>\n" +
//                        "<li>Emotional changes may range from weepiness, mood swings, anxiety, and excitement</li>\n" +
//                        "</ul><br />\n" +
//                        "<p><strong>Changes in a Woman&rsquo;s Daily Routine</strong>&nbsp;&ndash; Some of the changes that you experience in your first trimester may cause you to revise your daily activities. You may feel the need to go to sleep early or eat more frequently or eat smaller portion of meals. Some women experience a lot of discomfort, and others may not feel any at all. Each pregnancy is unique and even if you&rsquo;ve been pregnant before you may<br />feel completely different with each subsequent pregnancy.</p>"));
        return jsonFeedList;
    }
}
