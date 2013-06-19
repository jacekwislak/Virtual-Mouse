#include <linux/module.h>
#include <linux/init.h>
#include <linux/in.h>
#include <net/sock.h>
#include <linux/skbuff.h>
#include <linux/delay.h>
#include <linux/inet.h>
#include <linux/fs.h>
#include <asm/uaccess.h>
#include <linux/pci.h>
#include <linux/input.h>
#include <linux/platform_device.h>

#define SERVER_PORT 5555

/* Struktura reprezentujaca urzedzenie wejsciowe */
struct input_dev *vms_input_dev;
/* Struktura reprezetujaca urzedzenie */
static struct platform_device *vms_dev;
/* Struktura reprezentująca gniazdo (socket) */
static struct socket *udpsocket=NULL;
/* Struktura reprezentująca kolejke danych odbieranych przez socket */
struct workqueue_struct *wq;

static DECLARE_COMPLETION( threadcomplete );

/* Struktura pomocnicza do zarządzania workqueue_struct i socketem */
struct wq_wrapper{
        struct work_struct worker;
	struct sock * sk;
};

struct wq_wrapper wq_data;
/////////////////////////////////////////////////////////////////////

static void cb_data(struct sock *sk, int bytes){
	wq_data.sk = sk;
	queue_work(wq, &wq_data.worker);
}

short int bin_to_int(unsigned char *p, int index) {
	short int liczba=0;
	liczba=liczba|(p[index]<<8);
	liczba=liczba|p[index+1];
	return liczba;
}

void int_to_bin(short int liczba, unsigned char *tab) {
	tab[0]=liczba>>8;
	tab[1]=liczba;
}

void send_answer(struct work_struct *data){
	struct  wq_wrapper * foo = container_of(data, struct  wq_wrapper, worker);
	int len = 0;
	int x,y;
	/* Dopoki sa jakies nie przetworzone pakiety petla jest wykonywana */
	while((len = skb_queue_len(&foo->sk->sk_receive_queue)) > 0){
		struct sk_buff *skb = NULL;

		/* odebranie pakietu */
		skb = skb_dequeue(&foo->sk->sk_receive_queue);
		x=bin_to_int(skb->data+8,0);
		y=bin_to_int(skb->data+8,2);
		
		//////////////////SSTEROWANIE MYSZKA////////////////
		if (x==32767) {
			input_report_key(vms_input_dev, BTN_0, 1);
			input_report_key(vms_input_dev, BTN_0, 0);
		}		
		else if (y==32767) {
			input_report_key(vms_input_dev, BTN_2, 1);
			input_report_key(vms_input_dev, BTN_2, 0);
		}	
		else {
			input_report_rel(vms_input_dev, REL_X, x);
			input_report_rel(vms_input_dev, REL_Y, y);
		}

		input_sync(vms_input_dev);
		//////////////////////////////////////////////////////

		/* zwolnij pamiec zarezerwowana dla pakietu skb */
		kfree_skb(skb);
	}
}

static int __init server_init( void )
{
	////////////SOCKET///////////////////
	struct sockaddr_in server;
	int servererror;
	printk("INIT MODULE\n");
	///////socket do odbierania danych///////
	if (sock_create(PF_INET, SOCK_DGRAM, IPPROTO_UDP, &udpsocket) < 0) {
		printk( KERN_ERR "server: Error creating udpsocket.n" );
		return -EIO;
	}
	server.sin_family = AF_INET;
	server.sin_addr.s_addr = INADDR_ANY;
	server.sin_port = htons( (unsigned short)SERVER_PORT);
	servererror = udpsocket->ops->bind(udpsocket, (struct sockaddr *) &server, sizeof(server ));
	if (servererror) {
		sock_release(udpsocket);
		printk( KERN_ERR "server: blad przy bindowaniu portu" );
		return -EIO;
	}
	udpsocket->sk->sk_data_ready = cb_data;

	/* utworzenie work queue */	
	INIT_WORK(&wq_data.worker, send_answer);
	wq = create_singlethread_workqueue("myworkqueue"); 
	if (!wq){
		return -ENOMEM;
	}

	//////////////////MYSZKA//////////////
	/* Zarejestruj urządzenie w systemie */
	vms_dev = platform_device_register_simple("vms", -1, NULL, 0);
	if (IS_ERR(vms_dev)){
	printk ("vms_init: error\n");
	return PTR_ERR(vms_dev);
	}

	/* Zainicjuj strukture input_dev */
	vms_input_dev = input_allocate_device();
	if (!vms_input_dev) {
		printk("Bad input_allocate_device()\n"); return -ENOMEM;
	}

	/* Przypisz input device unikalną nazwe */
	vms_input_dev->name="remote mouse";

	/* Ustawienie zdarzen generowanych przez urzadzenie */
	set_bit(EV_REL, vms_input_dev->evbit);
	set_bit(EV_KEY, vms_input_dev->evbit);
	set_bit(BTN_0, vms_input_dev->keybit);
	set_bit(BTN_2, vms_input_dev->keybit);
	set_bit(REL_X, vms_input_dev->relbit);
	set_bit(REL_Y, vms_input_dev->relbit);

	/* Zarejestruj urządzenie wejsciowe */
	input_register_device(vms_input_dev);

	printk("WIRTUALNA MYSZKA ZAINICJALIZOWANA.\n");

	return 0;
}

static void __exit server_exit( void )
{

	//////////////////SOCKET////////////////
	if (udpsocket)
		sock_release(udpsocket);

	if (wq) {
                flush_workqueue(wq);
                destroy_workqueue(wq);
	}

	//////////////MYSZKA////////////////////
	/* Wyrejestruj urządzenie wejsciowe */
	input_unregister_device(vms_input_dev);

	/* Wyrejestruj urzadzenie */
	platform_device_unregister(vms_dev);

	printk("KONIEC PROGRAMU");
}

module_init(server_init);
module_exit(server_exit);
MODULE_LICENSE("GPL");
